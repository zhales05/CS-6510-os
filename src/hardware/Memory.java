package hardware;

import util.ErrorDump;
import util.VerboseModeLogger;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class Memory implements Hardware {
    private static final int TOTAL_SIZE = 100000;
    private static Memory instance;

    private final byte[] memory = new byte[TOTAL_SIZE];
    private int pc = 0;

    private final VerboseModeLogger logger = VerboseModeLogger.getInstance();

    private Memory() {
    }

    public static Memory getInstance() {
        if (instance == null) {
            instance = new Memory();
        }

        return instance;
    }

    public void load(byte[] program) {
        ByteBuffer bb = ByteBuffer.wrap(program);
        bb.order(ByteOrder.LITTLE_ENDIAN);

        if(!validateLoad(program)){
            return;
        }

        int programSize = bb.getInt();
        logger.print("Program size: " + programSize);
        pc = bb.getInt();
        logger.print("PC: " + pc);
        int index = bb.getInt();
        logger.print("Index: " + index);

        if (programSize > TOTAL_SIZE) {
            ErrorDump.getInstance().logError("Program size exceeds memory capacity");
            return;
        }

        logger.print("Copying program to memory");
        System.arraycopy(program, 12, memory, pc, programSize);
        logger.print(coreDump());
        index = programSize;
    }

    private boolean validateLoad(byte[] program) {
        if(program.length < 12) {
            ErrorDump.getInstance().logError("Program size is less than 12 bytes");
            return false;
        }

        if(pc > 0) {
            ErrorDump.getInstance().logError("Program already loaded. Please call clear.");
            return false;
        }

        return true;
    }

    public void clear() {
        Arrays.fill(memory, (byte) 0);
        pc = 0;
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
