package vm.hardware;

import os.OperatingSystem;
import os.ProcessControlBlock;
import os.ProcessStatus;
import os.queues.QueueId;
import os.util.ErrorDump;
import os.util.Logging;

public class Cpu implements Logging {
    private static Cpu instance;
    private final Memory memory = Memory.getInstance();
    private boolean idle = true;
    private boolean kernelMode = false;

    private ProcessControlBlock currentPcb;
    private final int[] registers = new int[12];
    private ProcessControlBlock currentProcess;

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

    public static Cpu getInstance() {
        if (instance == null) {
            instance = new Cpu();
        }
        return instance;
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

    public ProcessControlBlock getCurrentProcess() {
        return currentProcess;
    }

    public void setRegisters(int[] registers) {
        System.arraycopy(registers, 0, this.registers, 0, this.registers.length);
    }

    public int[] getRegisters() {
        return registers;
    }

    public void run(ProcessControlBlock pcb, OperatingSystem os) {
        if (pcb != null && pcb.getStatus() != ProcessStatus.TERMINATED) {
            pcb.setStatus(ProcessStatus.RUNNING, QueueId.RUNNING_QUEUE);
            currentProcess = pcb;
            setProgramCounter(pcb.getPc());
            setRegisters(pcb.getRegisters());
            try {
                while (getProgramCounter() < Memory.getInstance().getTotalSize()) {
                    byte instruction = Memory.getInstance().getByte();

                    if(idle){
                        return;
                    }

                    switch (instruction) {
                        case ADD:
                            log("ADD");
                            int resultR = Memory.getInstance().getByte();
                            int val1R = Memory.getInstance().getByte();
                            int val2R = Memory.getInstance().getByte();
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
                            int subR = Memory.getInstance().getByte();
                            int sub1R = Memory.getInstance().getByte();
                            int sub2R = Memory.getInstance().getByte();
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
                            int mulR = Memory.getInstance().getByte();
                            int mul1R = Memory.getInstance().getByte();
                            int mul2R = Memory.getInstance().getByte();
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
                            int divR = Memory.getInstance().getByte();
                            int div1R = Memory.getInstance().getByte();
                            int div2R = Memory.getInstance().getByte();
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
                            int r = Memory.getInstance().getByte();
                            int val = Memory.getInstance().getInt();
                            if (isNotValidRegisters(r)) {
                                return;
                            }
                            registers[r] = val;
                            log("Register: " + r + " Value: " + val);
                            break;
                        case MOV:
                            log("MOV");
                            int dest = Memory.getInstance().getByte();
                            int src = Memory.getInstance().getByte();
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
                            int destR = Memory.getInstance().getByte();
                            int srcR = Memory.getInstance().getByte();
                            addToPC(3);
                            if (isNotValidRegisters(destR, srcR)) {
                                return;
                            }
                            Memory.getInstance().setInt((byte) registers[srcR], registers[destR]);
                            log("Register: " + srcR + " Value: " + registers[srcR]);
                            log("Register: " + destR + " Value: " + registers[destR]);
                            break;
                        case END:
                            pcb.setRegisters(registers);
                            os.terminateProcess(pcb);
                            idle = true;
                            return;
                        default:
                            logError("Process " + currentPcb.getPid() + ": Invalid instruction " + instruction);
                            return;
                    }
                    Clock.getInstance().tick();
                }
            } catch (Exception e) {
                ErrorDump.getInstance().logError("Error executing instruction: " + e.getMessage());
                e.printStackTrace();
            }
            pcb.setRegisters(registers);
            pcb.setPc(getProgramCounter());
            currentProcess = null;
        }
    }

    private void swi(OperatingSystem os, ProcessControlBlock pcb) {
        int c = Memory.getInstance().getInt();
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
                Clock.getInstance().tick(20); // Simplified to fixed wait time
                break;
            case 4:
                log("io");
                os.addToIOQueue(currentPcb);
                break;
            case 5:
                log("Write to shared memory from register 0");
                os.writeToSharedMemory(registers[0]);
                break;
            case 6:
                log("Read from shared memory to register 0");
                registers[0] = os.readFromSharedMemory();
                break;
            case 7:
                log("Unlink from shared memory");
                os.unlinkFromSharedMemory();
                break;
            default:
                logError("Process: " + pcb.getPid() + " Invalid SWI call");
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
        setRegisters(parent.getRegisters()); // Simplified
    }

    public void transition(ProcessControlBlock next) {
        if (next != null) {
            next.setStatus(ProcessStatus.RUNNING, QueueId.RUNNING_QUEUE);
            currentProcess = next;
            setProgramCounter(next.getPc());
            setRegisters(next.getRegisters());
        } else {
            currentProcess = null;
        }
    }

    public boolean isIdle() {
        return idle;
    }

    public void stopProcess() {
        currentProcess = null;
    }
}
