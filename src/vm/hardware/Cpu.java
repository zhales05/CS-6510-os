package vm.hardware;

import os.util.Logging;

public class Cpu implements Logging {
    private static Cpu instance;
    private final Memory memory = Memory.getInstance();
    private boolean kernelMode = false;

    private final int[] registers = new int[12];

    static final int MOV = 1;
    static final int STR = 2;
    static final int BX = 6;
    static final int ADD = 16;
    static final int SUB = 17;
    static final int MUL = 18;
    static final int DIV = 19;
    static final int SWI = 20;
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
        log(kernelMode ? "Kernel mode" : "User mode");
        this.kernelMode = kernelMode;
    }

    public static Cpu getInstance() {
        if (instance == null) {
            instance = new Cpu();
        }

        return instance;
    }

    private boolean validateRegister(int r) {
        boolean valid = r >= 0 && r < registers.length;
        if(!valid){
            logError("Invalid register: " + r);
        }

        return valid;
    }

    private boolean isNotValidRegisters(int... rs) {
        for(int r : rs){
            if(!validateRegister(r)){
                return true;
            }
        }

        return false;
    }

    public void run() {
        log(isKernelMode() ? "Kernel mode" : "User mode");

        while (true) {
            int curr = memory.getByte();
            switch (curr) {

                case ADD:
                    log("ADD");

                    int resultR = memory.getByte();
                    int val1R = memory.getByte();
                    int val2R = memory.getByte();

                    addToPC(2);

                    if(isNotValidRegisters(resultR, val1R, val2R)){
                        return;
                    }

                    registers[resultR] = registers[val1R] + registers[val2R];

                    log("Register: " + val1R + " Value: " + registers[val1R]);
                    log("Register: " + val2R + " Value: " + registers[val2R]);
                    log(registers[val1R] + " + " + registers[val2R] + " = " + registers[resultR]);
                    break;
                case SUB:
                    log("SUB");

                    int subR = memory.getByte();
                    int sub1R = memory.getByte();
                    int sub2R = memory.getByte();

                    addToPC(2);

                    if(isNotValidRegisters(subR, sub1R, sub2R)){
                        return;
                    }

                    registers[subR] = registers[sub1R] - registers[sub2R];

                    log("Register: " + sub1R + " Value: " + registers[sub1R]);
                    log("Register: " + sub2R + " Value: " + registers[sub2R]);
                    log(registers[sub1R] + " - " + registers[sub2R] + " = " + registers[subR]);
                    break;
                case MUL:
                    log("MUL");

                    int mulR = memory.getByte();
                    int mul1R = memory.getByte();
                    int mul2R = memory.getByte();

                    addToPC(2);

                    if(isNotValidRegisters(mulR, mul1R, mul2R)){
                        return;
                    }

                    registers[mulR] = registers[mul1R] * registers[mul2R];

                    log("Register: " + mul1R + " Value: " + registers[mul1R]);
                    log("Register: " + mul2R + " Value: " + registers[mul2R]);
                    log(registers[mul1R] + " * " + registers[mul2R] + " = " + registers[mulR]);
                    break;
                case DIV:
                    log("DIV");

                    int divR = memory.getByte();
                    int div1R = memory.getByte();
                    int div2R = memory.getByte();

                    addToPC(2);

                    if(isNotValidRegisters(divR, div1R, div2R)){
                        return;
                    }

                    registers[divR] = registers[div1R] / registers[div2R];

                    log("Register: " + div1R + " Value: " + registers[div1R]);
                    log("Register: " + div2R + " Value: " + registers[div2R]);
                    log(registers[div1R] + " / " + registers[div2R] + " = " + registers[divR]);
                    break;
                case SWI:
                    log("SWI");
                    swi();
                    break;
                case MVI:
                    log("MVI");

                    int r = memory.getByte();
                    int val = memory.getInt();

                    if(isNotValidRegisters(r)){
                        return;
                    }

                    registers[r] = val;

                    log("Register: " + r + " Value: " + val);
                    break;
                case MOV:
                    log("MOV");

                    int dest = memory.getByte();
                    int src = memory.getByte();
                    addToPC(3);

                    if(isNotValidRegisters(dest, src)){
                        return;
                    }

                    registers[dest] = registers[src];
                    log("Register: " + src + " Value: " + registers[src]);
                    log("Register: " + dest + " Value: " + registers[dest]);
                    break;
                case STR:
                    log("STR");

                    int destR = memory.getByte();
                    int srcR = memory.getByte();
                    addToPC(3);

                    if(isNotValidRegisters(destR, srcR)){
                        return;
                    }

                    memory.setInt((byte) registers[srcR], registers[destR]);
                    log("Register: " + srcR + " Value: " + registers[srcR]);
                    log("Register: " + destR + " Value: " + registers[destR]);
                    break;

                case END:
                    log("Program ended");
                    return;

                default:
                    log("Invalid instruction");
                    logError("Invalid instruction");
                    return;
            }
        }
    }

    private void swi() {
        int c = memory.getInt();
        setKernelMode(true);
        switch(c){
            case 0:
                log("Printing register 0");
                System.out.println("Register 0: " + registers[0]);
                addToPC(1);
                break;
            default:
                logError("Invalid SWI call");
                break;
        }
        setKernelMode(false);
    }
}
