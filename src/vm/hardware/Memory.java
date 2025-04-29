package vm.hardware;

import os.*;
import os.queues.QueueId;
import os.util.Logging;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Memory implements Logging {
    private static Memory instance;
    private static final int MEMORY_SIZE = 10000;

    private static final Cpu cpu = Cpu.getInstance();
    private static final Clock clock = Clock.getInstance();

    private final byte[] memory = new byte[MEMORY_SIZE];
    private int maxPagesPerProgram = Integer.MAX_VALUE;
    private List<ProcessControlBlock> processControlBlocks = new ArrayList<>();

    private Memory() {
    }

    public static Memory getInstance() {
        if (instance == null) {
            instance = new Memory();
        }

        return instance;
    }

    public static int getMemorySize() {
        return MEMORY_SIZE;
    }

    public byte getByte(ProcessControlBlock pcb) {
        int logicalAddress = cpu.getProgramCounter();
        int physicalAddress = translate(logicalAddress, pcb);
        cpu.addToPC(1);
        return memory[physicalAddress];
    }

    public int getInt(ProcessControlBlock pcb) {
        byte b0 = getByte(pcb);  // <- each getByte() increments PC by 1
        byte b1 = getByte(pcb);
        byte b2 = getByte(pcb);
        byte b3 = getByte(pcb);
        return ((b3 & 0xFF) << 24) |
                ((b2 & 0xFF) << 16) |
                ((b1 & 0xFF) << 8) |
                (b0 & 0xFF);
    }


    private int translate(int logicalAddress, ProcessControlBlock pcb) {
        int pageSize = VirtualMemoryManager.getPageSize();
        int pageNumber = logicalAddress / pageSize;
        int offset = logicalAddress % pageSize;

        PageTableEntry entry = pcb.getPageTable().getEntry(pageNumber);

        if (!entry.isValid()) {
            handlePageFault(pageNumber, pcb);
            entry = pcb.getPageTable().getEntry(pageNumber);
        }

        int frameNumber = entry.getFrameNumber();
        return frameNumber * pageSize + offset;
    }

    private void handlePageFault(int pageNumber, ProcessControlBlock pcb) {
        int resident = countResidentPages(pcb);
        int frame = -1;

        if (resident < pcb.getMaxPages()) {
            // still under quota → try to grab any free frame
            frame = FrameTable.getInstance().allocateFreeFrame();
            if (frame != -1) {
                log("Allocated free frame " + frame + " for P" + pcb.getPid() + "/vpn=" + pageNumber);
            }
        }

        if (frame == -1) {
            frame = evictFromSameProcess(pcb, pageNumber);
            if (frame == -1) {
                frame = evictFromOtherProcess(pcb);
            }

            if (frame == -1) {
                // out of memory globally
                logError("System out of memory: cannot evict any page. Terminating P" + pcb.getPid());
                pcb.setStatus(ProcessStatus.TERMINATED, QueueId.TERMINATED_QUEUE);
                return;
            }
            log("Reclaimed frame " + frame + " from P" + pcb.getPid());
        }

        // load the new page into 'frame'…
        byte[] pageBytes = pcb.getBackingStorePage(pageNumber);
        int pageSize = VirtualMemoryManager.getPageSize();
        int physStart = frame * pageSize;
        int toCopy = Math.min(pageBytes.length, pageSize);
        System.arraycopy(pageBytes, 0, memory, physStart, toCopy);

        // finally update its page‐table
        PageTableEntry e = pcb.getPageTable().getEntry(pageNumber);
        e.setFrameNumber(frame);
        e.setValid(true);
        log(fullCoreDump());
    }


    private int countResidentPages(ProcessControlBlock pcb) {
        PageTable pt = pcb.getPageTable();
        int count = 0;
        for (int vpn = 0; vpn < pt.getNumberOfPages(); vpn++) {
            if (pt.getEntry(vpn).isValid()) count++;
        }
        return count;
    }

    private int evictFromSameProcess(ProcessControlBlock pcb, int faultingVpn) {
        PageTable pt = pcb.getPageTable();
        for (int vpn = 0; vpn < pt.getNumberOfPages(); vpn++) {
            if (vpn == faultingVpn) continue;
            PageTableEntry e = pt.getEntry(vpn);
            if (e.isValid()) {
                int f = e.getFrameNumber();
                e.setValid(false);
                FrameTable.getInstance().freeFrame(f);
                return f;
            }
        }
        return -1;
    }

    private int evictFromOtherProcess(ProcessControlBlock requester) {
        for (ProcessControlBlock pcb : processControlBlocks) {
            if (pcb == requester) continue;  // skip self

            PageTable pt = pcb.getPageTable();
            for (int vpn = 0; vpn < pt.getNumberOfPages(); vpn++) {
                PageTableEntry e = pt.getEntry(vpn);
                if (e.isValid()) {
                    int f = e.getFrameNumber();
                    e.setValid(false);
                    FrameTable.getInstance().freeFrame(f);
                    log("Evicted frame " + f + " from P" + pcb.getPid());
                    return f;
                }
            }
        }
        return -1;  // no victim found globally
    }


    public ProcessControlBlock load(byte[] program, ProcessControlBlock pcb) {
        if (!validateLoad(program, pcb)) {
            return null;
        }

        ByteBuffer bb = ByteBuffer.wrap(program);
        bb.order(ByteOrder.LITTLE_ENDIAN);

        int programSize = bb.getInt();
        programSize += 1; //added end
        int programCounter = bb.getInt();
        int pageSize = VirtualMemoryManager.getPageSize();
        int pagesNeeded = (int) Math.ceil((double) programSize / pageSize);

        PageTable pageTable = new PageTable(pagesNeeded);

        for (int virtualPageNumber = 0; virtualPageNumber < pagesNeeded; virtualPageNumber++) {
            PageTableEntry entry = new PageTableEntry();
            entry.setValid(false); // not loaded into RAM yet

            pageTable.setEntry(virtualPageNumber, entry);
        }

        pcb.setPageTable(pageTable);
        pcb.setProgramSize(programSize);
        pcb.setBackingStore(program);
        pcb.setPc(programCounter);
        pcb.setProgramStart(0);
        pcb.setCodeStart(programCounter);
        pcb.setMaxPages(maxPagesPerProgram);
        processControlBlocks.add(pcb);
        clock.tick();

        return pcb;
    }


    private boolean validateLoad(byte[] program, ProcessControlBlock pcb) {
        if (program == null) {
            logError("Process: " + pcb.getPid() + " | " + "Program is null");
            return false;
        }

        if (program.length < 12) {
            logError("Process: " + pcb.getPid() + " | " + "Program size is less than 12 bytes");
            return false;
        }

        return true;
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
            }
        }

        log("Cleared memory and freed frames for Process " + pcb.getPid());
    }

    public String fullCoreDump() {
        StringBuilder sb = new StringBuilder();
        sb.append("Full System Core Dump:\n");

        int pageSize = VirtualMemoryManager.getPageSize();
        int totalFrames = VirtualMemoryManager.getTotalFrames();

        for (int frameNumber = 0; frameNumber < totalFrames; frameNumber++) {
            int start = frameNumber * pageSize;
            int end = Math.min(start + pageSize, memory.length);

            // Check if frame is all zeros
            boolean isEmpty = true;
            for (int i = start; i < end; i++) {
                if (memory[i] != 0) {
                    isEmpty = false;
                    break;
                }
            }

            if (isEmpty) {
                continue;  // Skip printing this frame
            }

            // Otherwise, print the frame
            sb.append("Frame ").append(frameNumber).append(":\n");
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

    public int getMaxPagesPerProgram() {
        return maxPagesPerProgram;
    }

    public void setMaxPagesPerProgram(int maxPagesPerProgram) {
        this.maxPagesPerProgram = maxPagesPerProgram;
    }
}
