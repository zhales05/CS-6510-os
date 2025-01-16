package hardware;

import util.ErrorDump;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class Memory implements Hardware {
    private static Memory instance;

    private static final int TOTAL_SIZE = 100000;

    private final byte[] memory = new byte[TOTAL_SIZE];
    private int pc = 0;

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

        int programSize = bb.getInt();
        pc = bb.getInt();
        int index = bb.getInt();


        if (programSize > TOTAL_SIZE) {
            ErrorDump.getInstance().logError("Program size exceeds memory capacity");
            return;
        }

        System.arraycopy(program, 12, memory, pc, programSize);
        index = programSize;
    }

    public String coreDump() {
        return "not implemented";
    }
}
