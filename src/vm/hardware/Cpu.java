package vm.hardware;

import vm.util.ErrorDump;
import vm.util.VerboseModeLogger;

public class Cpu implements Hardware {
    private static Cpu instance;

    private final VerboseModeLogger logger = VerboseModeLogger.getInstance();
    private final ErrorDump errorDump = ErrorDump.getInstance();
    private final Memory memory = Memory.getInstance();

    private boolean kernelMode = false;

    private final int[] registers = new int[12];

    static final int ADD = 16;
    static final int SUB = 17;
    static final int MVI = 22;
    static final int END = 99;

    private Cpu() {
    }

    public int getProgramCounter() {
        return registers[11];
    }

    public void setProgramCounter(int val) {
        registers[11] = val;
    }

    public void addToPC(int val) {
        registers[11] += val;
    }

    public boolean isKernelMode() {
        return kernelMode;
    }

    public void setKernelMode(boolean kernelMode) {
        this.kernelMode = kernelMode;
    }

    public static Cpu getInstance() {
        if (instance == null) {
            instance = new Cpu();
        }

        return instance;
    }

    public void run() {
        while (true) {
            int curr = memory.getByte();
            switch (curr) {
                case MVI:
                    int r = memory.getByte();
                    int val = memory.getInt();
                    registers[r] = val;

                    logger.print("MVI");
                    logger.print("Register: " + r + " Value: " + val);
                    break;
                case ADD:
                    int resultR = memory.getByte();
                    int val1R = memory.getByte();
                    int val2R = memory.getByte();

                    addToPC(2);
                    registers[resultR] = registers[val1R] + registers[val2R];

                    logger.print("ADD");
                    logger.print("Register: " + val1R + " Value: " + registers[val1R]);
                    logger.print("Register: " + val2R + " Value: " + registers[val2R]);
                    logger.print(registers[val1R] + " + " + registers[val2R] + " = " + registers[resultR]);
                    break;
                case SUB:
                    int subR = memory.getByte();
                    int sub1R = memory.getByte();
                    int sub2R = memory.getByte();

                    addToPC(2);
                    registers[subR] = registers[sub1R] - registers[sub2R];

                    logger.print("SUB");
                    logger.print("Register: " + sub1R + " Value: " + registers[sub1R]);
                    logger.print("Register: " + sub2R + " Value: " + registers[sub2R]);
                    logger.print(registers[sub1R] + " - " + registers[sub2R] + " = " + registers[subR]);
                    break;
                case END:
                    logger.print("Program ended");
                    return;
                default:
                    logger.print("Invalid instruction");
                    errorDump.logError("Invalid instruction");
                    return;
            }
        }
    }
}
