package vm.hardware;

import vm.Cpu;
import vm.util.ErrorDump;
import vm.util.VerboseModeLogger;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class Memory implements Hardware {
    private static final int TOTAL_SIZE = 100000;
    private static Memory instance;
    private static final Cpu cpu = Cpu.getInstance();
    private final byte[] memory = new byte[TOTAL_SIZE];

    private final VerboseModeLogger logger = VerboseModeLogger.getInstance();

    private Memory() {
    }

    public static Memory getInstance() {
        if (instance == null) {
            instance = new Memory();
        }

        return instance;
    }

    public byte getByte() {
        return memory[cpu.getProgramCounter()];
    }

    public int getInt(){
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

        int programSize = bb.getInt();
        logger.print("Program size: " + programSize);
        cpu.setProgramCounter(bb.getInt());
        logger.print("PC: " + cpu.getProgramCounter());
        int index = bb.getInt();
        logger.print("Index: " + index);

        if (programSize > TOTAL_SIZE) {
            ErrorDump.getInstance().logError("Program size exceeds memory capacity");
            return;
        }

        logger.print("Copying program to memory");
        System.arraycopy(program, 12, memory, index, programSize);
        logger.print(coreDump());
        index += programSize;
       // index += index % 6;
        memory[index++] = (byte) 99;
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

        if(cpu.getProgramCounter() > 0) {
            ErrorDump.getInstance().logError("Program already loaded. Please call clear.");
            return false;
        }

        return true;
    }

    public void clear() {
        Arrays.fill(memory, (byte) 0);
        cpu.setProgramCounter(0);
        logger.print("Memory cleared");
    }

    //TODO: need to core dump in a more organized way - line break for every 6 bytes?
    public String coreDump() {
        StringBuilder sb = new StringBuilder();
        for (byte b : memory) {
            sb.append(b);
            sb.append(" ");
        }

        return sb.toString();
    }
}
