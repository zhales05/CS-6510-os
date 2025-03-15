package vm.hardware;

import os.ProcessControlBlock;
import os.util.Logging;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class Memory implements Logging {
    private static final int TOTAL_SIZE = 100000;
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
        int loaderAddress = bb.getInt();

        //check if program size exceeds memory capacity
        if (programSize + loaderAddress > TOTAL_SIZE) {
            logError("Process: " + pcb.getPid() + "Program size exceeds memory capacity");
            return null;
        }

        //pc needs to be adjusted for loader address
        pcb.setPc(programCounter + loaderAddress);
        log("PC: " + pcb.getPc());

        //loading up PCB for future use
        pcb.setProgramStart(loaderAddress);
        pcb.setCodeStart(pcb.getPc());
        pcb.setPc(pcb.getPc());
        pcb.setProgramSize(programSize);

        log("Copying program to memory");
        System.arraycopy(program, 12, memory, loaderAddress, programSize);
        index = Math.max(index, loaderAddress + programSize);
        memory[loaderAddress + programSize] = (byte) Cpu.END;
        log(coreDump(pcb));
        clock.tick(1);
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

    /**
     * Returns the memory array for direct access.
     * This method is used by the LDR instruction to read values from memory.
     * 
     * @return The memory array
     */
    public byte[] getMemoryArray() {
        return memory;
    }
}
