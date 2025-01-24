package vm.hardware;

import os.util.ErrorDump;
import os.util.VerboseModeLogger;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class Memory implements Hardware {
    private static final int TOTAL_SIZE = 100000;
    private static Memory instance;

    private static final Cpu cpu = Cpu.getInstance();
    private static final VerboseModeLogger logger = VerboseModeLogger.getInstance();

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


    public void load(byte[] program) {
        if(!validateLoad(program)){
            return;
        }

        ByteBuffer bb = ByteBuffer.wrap(program);
        bb.order(ByteOrder.LITTLE_ENDIAN);

        //program size is first int in the program header
        int programSize = bb.getInt();
        logger.print("Program size: " + programSize);

        //program counter(pc) is second int in the program header
        cpu.setProgramCounter(bb.getInt());

        //index is third int in the program header
        index += bb.getInt();
        logger.print("Index: " + index);

        //pc needs to be adjusted for index
        cpu.addToPC(index);
        logger.print("PC: " + cpu.getProgramCounter());

        if (programSize + index > TOTAL_SIZE) {
            ErrorDump.getInstance().logError("Program size exceeds memory capacity");
            return;
        }

        logger.print("Copying program to memory");
        System.arraycopy(program, 12, memory, index, programSize);
        index += programSize;
        memory[index++] = (byte) Cpu.END;
        logger.print(coreDump());
    }

    private boolean validateLoad(byte[] program) {
        if(program == null) {
            ErrorDump.getInstance().logError("Program is null");
            return false;
        }

        if(program.length < 12) {
            ErrorDump.getInstance().logError("Program size is less than 12 bytes");
            return false;
        }

        return true;
    }

    public void clear() {
        Arrays.fill(memory, (byte) 0);
        cpu.setProgramCounter(0);
        index = 0;
        logger.print("Memory cleared");
    }

    public String coreDump() {
        StringBuilder sb = new StringBuilder();
        sb.append("Core Dump:\n");
        for (int i = cpu.getProgramCounter(); i < index; i++) {
            sb.append(memory[i]);
            sb.append(" ");
            if ((i - cpu.getProgramCounter() + 1) % 6 == 0) {
                sb.append("\n");
            }
        }

        return sb.toString();
    }
}
