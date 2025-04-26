package vm.hardware;

import os.*;
import os.util.Logging;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class Memory implements Logging {
    private static final int TOTAL_SIZE = 1000;
    private static Memory instance;

    private static final Cpu cpu = Cpu.getInstance();
    private static final Clock clock = Clock.getInstance();

    private final byte[] memory = new byte[TOTAL_SIZE];
    private int index = 0;
    private int clockHand = -1;


    private Memory() {
    }

    public static Memory getInstance() {
        if (instance == null) {
            instance = new Memory();
        }

        return instance;
    }

    //TODO-SHALES - will likely delete this
    public byte getByte() {
        byte b = memory[cpu.getProgramCounter()];
        cpu.addToPC(1);
        return b;
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

    public int getInt() {
        ByteBuffer bb = ByteBuffer.wrap(Arrays.copyOfRange(memory, cpu.getProgramCounter(), cpu.getProgramCounter() + 4));
        bb.order(ByteOrder.LITTLE_ENDIAN);
        int i = bb.getInt();
        cpu.addToPC(4);
        return i;
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


    public void setInt(byte location, int value) {
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(value);
        System.arraycopy(bb.array(), 0, memory, location, 4);
    }

    public void setByte(byte location, byte value) {
        memory[location] = value;
    }

    public byte peakByte() {
        return memory[cpu.getProgramCounter()];
    }

    public int peakInt() {
        ByteBuffer bb = ByteBuffer.wrap(Arrays.copyOfRange(memory, cpu.getProgramCounter(), cpu.getProgramCounter() + 4));
        bb.order(ByteOrder.LITTLE_ENDIAN);
        return bb.getInt();
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

            System.arraycopy(program, programByteIndex, memory, physicalAddress, bytesToCopy);
            programByteIndex += bytesToCopy;

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

        return true;
    }

    public void clear() {
        Arrays.fill(memory, (byte) 0);
        cpu.setProgramCounter(0);
        index = 0;
        log("Memory cleared");
    }

    public void clear(ProcessControlBlock pcb){
        Arrays.fill(memory, pcb.getProgramStart(), pcb.getProgramStart() + pcb.getProgramSize() + 1, (byte) 0);
       // log("Memory cleared for process " + pcb.getPid());
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

    public String coreDump() {
        return coreDump(0, index);
    }

    public int getMemorySize() {
        return TOTAL_SIZE;
    }
}
