package vm.hardware;

import os.OperatingSystem;
import os.ProcessControlBlock;
import os.util.Logging;

import java.util.Random;


public class Cpu implements Logging {
    private static Cpu instance;
    private final Memory memory = Memory.getInstance();
    private boolean idle = true;
    private boolean kernelMode = false;

    private ProcessControlBlock currentPcb;
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
        if (!valid) {
            logError("Invalid register: " + r);
        }

        return valid;
    }

    private boolean isNotValidRegisters(int... rs) {
        for (int r : rs) {
            if (!validateRegister(r)) {
                return true;
            }
        }

        return false;
    }

    private void loadRegistersFromPcb(ProcessControlBlock pcb) {
        System.arraycopy(pcb.getRegisters(), 0, registers, 0, registers.length);
        currentPcb = pcb;
    }

    public void run(ProcessControlBlock pcb, OperatingSystem os) {
        loadRegistersFromPcb(pcb);
        idle = false;

        while (true) {
            int curr = memory.getByte();

            if(idle){
                //if the cpu has been set to idle it has been stopped and we need to bail
                return;
            }

            switch (curr) {
                case ADD:
                    log("ADD");

                    int resultR = memory.getByte();
                    int val1R = memory.getByte();
                    int val2R = memory.getByte();

                    addToPC(2);

                    if (isNotValidRegisters(resultR, val1R, val2R)) {
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

                    if (isNotValidRegisters(subR, sub1R, sub2R)) {
                        return;
                    }

                    log("Register: " + sub1R + " Value: " + registers[sub1R]);
                    log("Register: " + sub2R + " Value: " + registers[sub2R]);

                    registers[subR] = registers[sub1R] - registers[sub2R];
                    log(registers[sub1R] + " - " + registers[sub2R] + " = " + registers[subR]);
                    break;
                case MUL:
                    log("MUL");
                    int mulR = memory.getByte();
                    int mul1R = memory.getByte();
                    int mul2R = memory.getByte();

                    addToPC(2);

                    if (isNotValidRegisters(mulR, mul1R, mul2R)) {
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

                    if (isNotValidRegisters(divR, div1R, div2R)) {
                        return;
                    }

                    registers[divR] = registers[div1R] / registers[div2R];
                    log("Register: " + div1R + " Value: " + registers[div1R]);
                    log("Register: " + div2R + " Value: " + registers[div2R]);
                    log(registers[div1R] + " / " + registers[div2R] + " = " + registers[divR]);
                    break;
                case SWI:
                    log("SWI");
                    swi(os, pcb);
                    break;
                case MVI:
                    log("MVI");
                    int r = memory.getByte();
                    int val = memory.getInt();

                    if (isNotValidRegisters(r)) {
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

                    if (isNotValidRegisters(dest, src)) {
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

                    if (isNotValidRegisters(destR, srcR)) {
                        return;
                    }

                    memory.setInt((byte) registers[srcR], registers[destR]);
                    log("Register: " + srcR + " Value: " + registers[srcR]);
                    log("Register: " + destR + " Value: " + registers[destR]);
                    break;

                case END:
                    currentPcb.setRegisters(registers);
                    os.terminateProcess(currentPcb);
                    idle = true;
                    return;

                default:
                    logError("Process " + currentPcb.getPid() + ": Invalid instruction " + curr);
                    return;
            }
            Clock.getInstance().tick();
        }
    }

    private void swi(OperatingSystem os, ProcessControlBlock pcb) {
        int c = memory.getInt();
        setKernelMode(true);
        addToPC(1);
        switch (c) {
            case 0:
                log("Printing register 0");
                System.out.println("Register 0: " + registers[0]);
                break;
            case 1:
                log("Printing register 1");
                System.out.println("Register 1: " + registers[1]);
                break;
            case 2:
                log("vfork");
                startChildProcess(os, pcb);
                break;
            case 3:
                log("wait");
                Random random = new Random();
                int randomTicks = random.nextInt(20) + 1;
                log("Waiting for " + randomTicks + " ticks");
                Clock.getInstance().tick(randomTicks);
                break;
            case 4:
                log("io");
                os.addToIOQueue(currentPcb);
                break;
            case 5:
                log("LOCK ENGAGED");
                os.lockSharedMemory();
                break;
            case 6:
                log("UNLOCK ENGAGED");
                os.unlockSharedMemory();
                break;
            default:
                logError("Process: " + pcb.getPid() + "Invalid SWI call");
                break;
        }
        setKernelMode(false);
    }

    private void startChildProcess(OperatingSystem os, ProcessControlBlock parent) {
        parent.setRegisters(registers);
        log("Starting child process");
        ProcessControlBlock child = os.startChildProcess(parent);
        parent.addChild(child);
        log("Back to parent");
        loadRegistersFromPcb(parent);
    }

    public void transition(ProcessControlBlock next) {
        currentPcb.setRegisters(registers);
        loadRegistersFromPcb(next);
    }

    public boolean isIdle() {
        return idle;
    }

    public void stopProcess() {
        currentPcb.setRegisters(registers);
        idle = true;
    }
}
