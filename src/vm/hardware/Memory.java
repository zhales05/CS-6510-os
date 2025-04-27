package vm.hardware;

import os.*;
import os.util.Logging;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class Memory implements Logging {
    private static Memory instance;

    private static final Cpu cpu = Cpu.getInstance();
    private static final Clock clock = Clock.getInstance();

    private byte[] memory;
    private int clockHand = -1;

    private int numberOfPages = 10;


    private Memory() {
        initializeMemory();
    }

    public static Memory getInstance() {
        if (instance == null) {
            instance = new Memory();
        }

        return instance;
    }

    public void setPageNumber(int numberOfPages) {
        this.numberOfPages = numberOfPages;
        initializeMemory();
    }

    public void initializeMemory() {
        int pageSize = VirtualMemoryManager.getPageSize();
        memory = new byte[pageSize * numberOfPages];
        log("Memory initialized with size: " + memory.length);
    }


    public byte getByte(ProcessControlBlock pcb) {
        int logicalAddress = cpu.getProgramCounter();
        int physicalAddress = translate(logicalAddress, pcb);
        cpu.addToPC(1);
        return memory[physicalAddress];
    }

    public int getInt(ProcessControlBlock pcb) {
        int logicalAddress = cpu.getProgramCounter();
        int physicalAddress = translate(logicalAddress, pcb);
        cpu.addToPC(4);

        ByteBuffer bb = ByteBuffer.wrap(Arrays.copyOfRange(memory, physicalAddress, physicalAddress + 4));
        bb.order(ByteOrder.LITTLE_ENDIAN);
        return bb.getInt();
    }

    private int translate(int logicalAddress, ProcessControlBlock pcb) {
        int pageSize = VirtualMemoryManager.getPageSize();
        int pageNumber = logicalAddress / pageSize;
        int offset = logicalAddress % pageSize;

        PageTableEntry entry = pcb.getPageTable().getEntry(pageNumber);

        if (!entry.isValid()) {
            handlePageFault(pageNumber, pcb);
            // now that it’s loaded, re-fetch the entry
            entry = pcb.getPageTable().getEntry(pageNumber);
        }

        int frameNumber = entry.getFrameNumber();
        return frameNumber * pageSize + offset;
    }

    private void handlePageFault(int pageNumber, ProcessControlBlock pcb) {
        logError("Page fault on page " + pageNumber + " in process " + pcb.getPid());

        // 1) Allocate a free frame (or pick a victim via LRU/ESCA)
        int frame = FrameTable.getInstance().allocateFreeFrame();
        if (frame == -1) {
            frame = evictFrame(pcb);  // your replacement algorithm
        }

        // 2) Load the page into that frame
        // You need a “backing store” reference—either
        //   a) keep the original program byte[] in the PCB
        //   b) ask the OS to read it from disk
        byte[] pageBytes = pcb.getBackingStorePage(pageNumber);
        int pageSize = VirtualMemoryManager.getPageSize();
        int physStart = frame * pageSize;
        System.arraycopy(pageBytes, 0, memory, physStart, pageBytes.length);

        // 3) Update the page table
        PageTableEntry entry = pcb.getPageTable().getEntry(pageNumber);
        entry.setFrameNumber(frame);
        entry.setValid(true);
        entry.setReferenceBit(true);
        entry.setDirtyBit(false);  // freshly loaded
    }

    /**
     * Evict one frame using ESCA (Enhanced Second-Chance).
     * Scans the running process's pages in four classes:
     *  (R=0,D=0) → (R=0,D=1) → clear R bits → (R=1,D=0) → (R=1,D=1)
     */
    private int evictFrame(ProcessControlBlock pcb) {
        PageTable pt = pcb.getPageTable();
        int pages = pt.getNumberOfPages();
        int totalFrames = VirtualMemoryManager.getTotalFrames();

        // four passes for ESCA classes 0→3
        for (int pass = 0; pass < 4; pass++) {
            for (int i = 0; i < pages; i++) {
                // advance clock hand
                clockHand = (clockHand + 1) % pages;
                PageTableEntry e = pt.getEntry(clockHand);

                if (!e.isValid()) continue;  // not in memory at all

                boolean R = e.isReferenceBit();
                boolean D = e.isDirtyBit();
                int cls = (R ? 2 : 0) + (D ? 1 : 0);

                // pass 0: look for cls==0 (0,0)
                // pass 1: look for cls==1 (0,1)
                // pass 2: clear all R bits, continue
                // pass 3: look for cls==2 or cls==3
                if (pass == 2) {
                    // clear reference bits on the fly
                    if (R) e.setReferenceBit(false);
                    continue;
                }

                if ( (pass == 0 && cls == 0)
                        || (pass == 1 && cls == 1)
                        || (pass == 3 && (cls == 2 || cls == 3)) )
                {
                    // we’ve found our victim!
                    int frame = e.getFrameNumber();

                    // if dirty, write back
                    if (D) {
                        byte[] pageData = new byte[VirtualMemoryManager.getPageSize()];
                        int start = frame * VirtualMemoryManager.getPageSize();
                        System.arraycopy(memory, start, pageData, 0, pageData.length);
                        pcb.writeBackPage(clockHand, pageData);
                    }

                    // invalidate the PTE and free the frame
                    e.setValid(false);
                    e.setReferenceBit(false);
                    e.setDirtyBit(false);
                    FrameTable.getInstance().freeFrame(frame);

                    return frame;
                }
            }
        }
        logError("evictFrame: no suitable page found to evict!");
        return -1;
    }

    public ProcessControlBlock load(byte[] program, ProcessControlBlock pcb) {
        if(!validateLoad(program, pcb)){
            return null;
        }
        ByteBuffer bb = ByteBuffer.wrap(program);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        log("Loading program " + pcb.getPid());
        //program size is first int in the program header
        int programSize = bb.getInt();
        log("Program size: " + programSize);

        //program counter(pc) is second int in the program header
        int programCounter = bb.getInt();

        //loader address is third int in the program header
        //int loaderAddress = bb.getInt();
        int pageSize = VirtualMemoryManager.getPageSize();
        int pagesNeeded = (int) Math.ceil((double) programSize / pageSize);

        PageTable pageTable = new PageTable(pagesNeeded);

        int programByteIndex = 12; // Program body starts after 3 ints (header)

        for (int virtualPageNumber = 0; virtualPageNumber < pagesNeeded; virtualPageNumber++) {
            int frameNumber = FrameTable.getInstance().allocateFreeFrame();
            if (frameNumber == -1) {
                logError("No free frames available for process: " + pcb.getPid());
                return null; // or handle page fault / out of memory situation
            }

            int physicalAddress = frameNumber * pageSize;
            int bytesToCopy = Math.min(pageSize, programSize - (virtualPageNumber * pageSize));

            if (physicalAddress + bytesToCopy > memory.length) {
                logError("Not enough physical memory to load page " + virtualPageNumber + " for process " + pcb.getPid());
                return null;
            }
            System.arraycopy(program, programByteIndex, memory, physicalAddress, bytesToCopy);
            programByteIndex += bytesToCopy;

            if (virtualPageNumber == pagesNeeded - 1) {
                // next byte after code
                memory[physicalAddress + bytesToCopy] = (byte)Cpu.END;
            }

            // Create PageTableEntry
            PageTableEntry entry = new PageTableEntry();
            entry.setFrameNumber(frameNumber);
            entry.setValid(true);
            entry.setReferenceBit(false);
            entry.setDirtyBit(false);

            pageTable.setEntry(virtualPageNumber, entry);
        }

        // Set PCB info
        pcb.setPageTable(pageTable);
        pcb.setProgramSize(programSize);
        pcb.setBackingStore(program);
        pcb.setPc(programCounter); // PC is still logical address now (typically 0)
        pcb.setProgramStart(0);    // Logical start is 0
        pcb.setCodeStart(programCounter);

        log(coreDump(pcb));
        clock.tick();

        return pcb;
    }

    private boolean validateLoad(byte[] program, ProcessControlBlock pcb) {
        if(program == null) {
            logError("Process: " + pcb.getPid() + " | " + "Program is null");
            return false;
        }

        if(program.length < 12) {
            logError("Process: " + pcb.getPid() + " | " + "Program size is less than 12 bytes");
            return false;
        }

        int pageSize = VirtualMemoryManager.getPageSize();
        int requiredPages = (int) Math.ceil((double)(program.length - 12) / pageSize);

        if (requiredPages > numberOfPages) {
            logError("Program too large to fit into memory. Required pages: " + requiredPages + ", available pages: " + numberOfPages);
            return false;
        }


        return true;
    }

    public void clear() {
        Arrays.fill(memory, (byte) 0);
        cpu.setProgramCounter(0);
        log("Memory cleared");
        clockHand = -1;
    }

    public void clear(ProcessControlBlock pcb) {
        PageTable pageTable = pcb.getPageTable();
        int pageSize = VirtualMemoryManager.getPageSize();

        for (int i = 0; i < pageTable.getNumberOfPages(); i++) {
            PageTableEntry entry = pageTable.getEntry(i);
            if (entry.isValid()) {
                int frameNumber = entry.getFrameNumber();
                int startAddress = frameNumber * pageSize;
                int endAddress = startAddress + pageSize;

                // Clear the memory contents of that frame
                Arrays.fill(memory, startAddress, endAddress, (byte) 0);

                // Free the frame in the frame table
                FrameTable.getInstance().freeFrame(frameNumber);

                // Invalidate the page table entry
                entry.setValid(false);
                entry.setReferenceBit(false);
                entry.setDirtyBit(false);
            }
        }

        log("Cleared memory and freed frames for Process " + pcb.getPid());
    }



    //temp trying to see if this is a better core dump
    public String fullCoreDump() {
        StringBuilder sb = new StringBuilder();
        sb.append("Full System Core Dump:\n");

        int pageSize = VirtualMemoryManager.getPageSize();
        int totalFrames = VirtualMemoryManager.getTotalFrames();

        for (int frameNumber = 0; frameNumber < totalFrames; frameNumber++) {
            sb.append("Frame ").append(frameNumber).append(":\n");

            int start = frameNumber * pageSize;
            int end = Math.min(start + pageSize, memory.length);

            for (int i = start; i < end; i++) {
                sb.append(memory[i]).append(" ");
                if ((i - start + 1) % 6 == 0) {
                    sb.append("\n");
                }
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    public String coreDump(int start, int end) {
        StringBuilder sb = new StringBuilder();
        sb.append("Core Dump:\n");
        for (int i = start; i < end; i++) {
            sb.append(memory[i]);
            sb.append(" ");
            if ((i - start + 1) % 6 == 0) {
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    public String coreDump(ProcessControlBlock pcb) {
        return coreDump(pcb.getCodeStart(), pcb.getCodeStart() + pcb.getProgramSize());
    }

    public int getMemorySize() {
        return memory.length;
    }
}
