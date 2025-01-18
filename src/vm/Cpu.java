package vm;

import vm.hardware.Memory;
import vm.util.ErrorDump;
import vm.util.VerboseModeLogger;

public class Cpu {
    private final VerboseModeLogger logger = VerboseModeLogger.getInstance();
    private final ErrorDump errorDump = ErrorDump.getInstance();
    private final Memory memory = Memory.getInstance();

    private static Cpu instance;
    private int[] registers = new int[12];

    private static final int ADD = 16;
    private static final int MVI = 22;

    private static final int END = 99;

    private Cpu() {
    }

    public int getProgramCounter(){
        return registers[11];
    }

    public void setProgramCounter(int val) {
        registers[11] = val;
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
            setProgramCounter(getProgramCounter() + 1);
            switch (curr) {
                case MVI:
                    int r = memory.getByte();
                    setProgramCounter(getProgramCounter() + 1);
                    int val = memory.getInt();
                    registers[r] = val;
                    setProgramCounter(getProgramCounter() + 4);
                    break;
                case ADD:
                    int resultR = memory.getByte();
                    setProgramCounter(getProgramCounter() + 1);
                    int val1R = memory.getByte();
                    setProgramCounter(getProgramCounter() + 1);
                    int val2R = memory.getByte();
                    setProgramCounter(getProgramCounter() + 3);

                    registers[resultR] = registers[val1R] + registers[val2R];

                    logger.print("Adding");
                    logger.print("Register: " + val1R + " Value: " + registers[val1R]);
                    logger.print("Register: " + val2R + " Value: " + registers[val2R]);
                    logger.print("Result Register: " + resultR);
                    logger.print(registers[val1R] + " + " + registers[val2R] + " = " + registers[resultR]);
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

    public void execute() {
        System.out.println("Executing...");
    }
}
