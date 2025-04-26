package vm.hardware;

import os.*;
import os.util.Logging;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class Memory implements Logging {
    private static final int TOTAL_SIZE = 10000;
    private static Memory instance;

    private static final Cpu cpu = Cpu.getInstance();
    private static final Clock clock = Clock.getInstance();

    private final byte[] memory = new byte[TOTAL_SIZE];
    private int index = 0;

    private Memory() {
    }

    public static Memory getInstance() {
        if (instance == null) {
            instance = new Memory();
        }

        return instance;
    }

    public byte getByte() {
        byte b = memory[cpu.getProgramCounter()];
        cpu.addToPC(1);
        return b;
    }

    public int getInt(){
        ByteBuffer bb = ByteBuffer.wrap(Arrays.copyOfRange(memory, cpu.getProgramCounter(), cpu.getProgramCounter() + 4));
        bb.order(ByteOrder.LITTLE_ENDIAN);
        int i = bb.getInt();
        cpu.addToPC(4);
        return i;
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
