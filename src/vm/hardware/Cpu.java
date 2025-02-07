package vm.hardware;

import java.io.IOException;

import os.ProcessControlBlock;
import os.ProcessStatus;
import os.Scheduler;
import os.util.Logging;

public class Cpu implements Logging {
    private static Cpu instance;
    private final Memory memory = Memory.getInstance();
    private boolean kernelMode = false;
    private final int[] registers = new int[12];
    private int stackPointer = 1000;
    private boolean zeroFlag = false;
    private boolean negativeFlag = false;
    private boolean positiveFlag = false;

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
    static final int AND = 23;
    static final int OR = 24;
    static final int XOR = 25;
    static final int NOT = 26;
    static final int CMP = 27;
    static final int BZ = 28;
    static final int BNZ = 29;
    static final int BP = 30;
    static final int BN = 31;
    static final int JMP = 32;
    static final int PUSH = 33;
    static final int POP = 34;
    static final int CALL = 35;
    static final int RET = 36;
    static final int LDR = 37;
    static final int STRB = 38;
    static final int FORK = 39;
    static final int WAIT = 40;
    static final int EXIT = 41;
    static final int IN = 42;
    static final int OUT = 43;
    static final int ADR = 44;
    static final int B = 45;
    static final int BNE = 46;

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

    private void loadRegistersFromPcb(ProcessControlBlock pcb) {
        for(int i = 0; i < registers.length-1; i++){
            registers[i] = pcb.getRegisters()[i];
        }
        registers[11] = pcb.getCodeStart();
    }

    private ProcessControlBlock currentPCB;
    
    public ProcessControlBlock getCurrentPCB() {
        return currentPCB;
    }

    public void setCurrentPCB(ProcessControlBlock pcb) {
        this.currentPCB = pcb;
    }

    private static int nextPid = 1;

    private int generateNewPID() {
        return nextPid++;
    }

    private void fork() {
    setKernelMode(true);
    ProcessControlBlock parentPCB = getCurrentPCB();
    ProcessControlBlock childPCB = new ProcessControlBlock(generateNewPID());

    // Copy register values
    System.arraycopy(parentPCB.getRegisters(), 0, childPCB.getRegisters(), 0, parentPCB.getRegisters().length);
    childPCB.setPc(parentPCB.getPc());

    // Associate parent and child processes
    parentPCB.addChild(childPCB.getPid());  // Fix: Track child process
    childPCB.setParentPid(parentPCB.getPid()); // Fix: Track parent process

    Scheduler.getInstance().addToReadyQueue(childPCB);  // Fix: Use scheduler for process management

    // Set return values
    parentPCB.setReturnValue(childPCB.getPid());
    childPCB.setReturnValue(0);

    log("Forked new process with PID: " + childPCB.getPid());
    setKernelMode(false);
    }


    private void waitProcess() {
    setKernelMode(true);
    ProcessControlBlock parentPCB = getCurrentPCB();

    if (parentPCB.hasChildren()) {
        parentPCB.setStatus(ProcessStatus.WAITING); // Mark process as waiting
        Scheduler.getInstance().blockProcess(parentPCB);  // Fix: Implement blocking in Scheduler
    } else {
        log("Wait called, but no child processes exist.");
    }

    setKernelMode(false);
    }


    public void run(ProcessControlBlock pcb) {
        loadRegistersFromPcb(pcb);

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

                case AND:
                    log("AND");
                    int andR = memory.getByte();
                    int and1R = memory.getByte();
                    int and2R = memory.getByte();
                    addToPC(2);
                    if(isNotValidRegisters(andR, and1R, and2R)) {
                        return;
                    }
                    registers[andR] = registers[and1R] & registers[and2R];
                    log("Register: " + and1R + " Value: " + registers[and1R]);
                    log("Register: " + and2R + " Value: " + registers[and2R]);
                    log(registers[and1R] + " & " + registers[and2R] + " = " + registers[andR]);
                    break;

                case OR:
                    log("OR");
                    int orR = memory.getByte();
                    int or1R = memory.getByte();
                    int or2R = memory.getByte();
                    addToPC(2);
                    if(isNotValidRegisters(orR, or1R, or2R)) {
                        return;
                    }
                    registers[orR] = registers[or1R] | registers[or2R];
                    log("Register: " + or1R + " Value: " + registers[or1R]);
                    log("Register: " + or2R + " Value: " + registers[or2R]);
                    log(registers[or1R] + " | " + registers[or2R] + " = " + registers[orR]);
                    break;

                case XOR:
                    log("XOR");
                    int xorR = memory.getByte();
                    int xor1R = memory.getByte();
                    int xor2R = memory.getByte();
                    addToPC(2);
                    if(isNotValidRegisters(xorR, xor1R, xor2R)) {
                        return;
                    }
                    registers[xorR] = registers[xor1R] ^ registers[xor2R];
                    log("Register: " + xor1R + " Value: " + registers[xor1R]);
                    log("Register: " + xor2R + " Value: " + registers[xor2R]);
                    log(registers[xor1R] + " ^ " + registers[xor2R] + " = " + registers[xorR]);
                    break;

                case NOT:
                    log("NOT");
                    int notDestR = memory.getByte();
                    int notSrcR = memory.getByte();
                    addToPC(1);
                    if(isNotValidRegisters(notDestR, notSrcR)) {
                        return;
                    }
                    registers[notDestR] = ~registers[notSrcR];
                    log("Register: " + notSrcR + " Value: " + registers[notSrcR]);
                    log("~" + registers[notSrcR] + " = " + registers[notDestR]);
                    break;

                case CMP:
                    log("CMP");
                    int cmp1R = memory.getByte();
                    int cmp2R = memory.getByte();
                    addToPC(1);
                    if(isNotValidRegisters(cmp1R, cmp2R)) {
                        return;
                    }
                    int result = registers[cmp1R] - registers[cmp2R];
                    zeroFlag = (result == 0);
                    negativeFlag = (result < 0);
                    positiveFlag = (result > 0);
                    log("Comparing R" + cmp1R + "(" + registers[cmp1R] + ") with R" + cmp2R + "(" + registers[cmp2R] + ")");
                    break;

                case BZ:
                    log("BZ");
                    int bzAddr = memory.getInt();
                    if (zeroFlag) {
                        setProgramCounter(bzAddr);
                    } else {
                        addToPC(3);
                    }
                    break;

                case BNZ:
                    log("BNZ");
                    int bnzAddr = memory.getInt();
                    if (!zeroFlag) {
                        setProgramCounter(bnzAddr);
                    } else {
                        addToPC(3);
                    }
                    break;

                case BP:
                    log("BP");
                    int bpAddr = memory.getInt();
                    if (positiveFlag) {
                        setProgramCounter(bpAddr);
                    } else {
                        addToPC(3);
                    }
                    break;

                case BN:
                    log("BN");
                    int bnAddr = memory.getInt();
                    if (negativeFlag) {
                        setProgramCounter(bnAddr);
                    } else {
                        addToPC(3);
                    }
                    break;

                case JMP:
                    log("JMP");
                    int jmpAddr = memory.getInt();
                    setProgramCounter(jmpAddr);
                    break;

                case PUSH:
                    log("PUSH");
                    int pushR = memory.getByte();
                    addToPC(1);
                    if(isNotValidRegisters(pushR)) {
                        return;
                    }
                    memory.setInt((byte) stackPointer, registers[pushR]);
                    stackPointer -= 4;
                    log("Pushed value " + registers[pushR] + " to stack");
                    break;

                case POP:
                    log("POP");
                    int popR = memory.getByte();
                    addToPC(1);
                    if(isNotValidRegisters(popR)) {
                        return;
                    }
                    stackPointer += 4;
                    registers[popR] = memory.getIntAtAddress(stackPointer);
                    log("Popped value " + registers[popR] + " from stack");
                    break;

                case CALL:
                    log("CALL");
                    int callAddr = memory.getInt();
                    memory.setInt((byte) stackPointer, getProgramCounter() + 3);
                    stackPointer -= 4;
                    setProgramCounter(callAddr);
                    break;

                case RET:
                    log("RET");
                    stackPointer += 4;
                    setProgramCounter(memory.getIntAtAddress(stackPointer));
                    break;

                case LDR:
                    log("LDR");
                    int ldrDestR = memory.getByte();
                    int ldrAddrR = memory.getByte();
                    addToPC(1);
                    if(isNotValidRegisters(ldrDestR, ldrAddrR)) {
                        return;
                    }
                    registers[ldrDestR] = memory.getIntAtAddress(registers[ldrAddrR]);
                    log("Loaded value " + registers[ldrDestR] + " from address " + registers[ldrAddrR]);
                    break;

                case STRB:
                    log("STRB");
                    int strbDestR = memory.getByte();
                    int strbSrcR = memory.getByte();
                    addToPC(1);
                    if(isNotValidRegisters(strbDestR, strbSrcR)) {
                        return;
                    }
                    memory.setByte((byte)registers[strbDestR], (byte)registers[strbSrcR]);
                    log("Stored byte " + registers[strbSrcR] + " to address " + registers[strbDestR]);
                    break;

                case FORK:
                    log("FORK");
                    setKernelMode(true);
                    fork();
                    setKernelMode(false);
                    addToPC(1);
                    break;

                case WAIT:
                    log("WAIT");
                    setKernelMode(true);
                    waitProcess();
                    setKernelMode(false);
                    addToPC(1);
                    break;

                case EXIT:
                    log("EXIT");
                    pcb.setRegisters(registers);
                    return;

                case IN:
                    log("IN");
                    int inR = memory.getByte();
                    addToPC(1);
                    if(isNotValidRegisters(inR)) {
                        return;
                    }
                    try {
                        registers[inR] = System.in.read();
                    } catch (IOException e) {
                        logError("Error reading input: " + e.getMessage());
                        registers[inR] = -1; // Assign default value in case of error
                    }
                    log("Input value " + registers[inR] + " stored in R" + inR);
                    break;

                case OUT:
                    log("OUT");
                    int outR = memory.getByte();
                    addToPC(1);
                    if(isNotValidRegisters(outR)) {
                        return;
                    }
                    System.out.println(registers[outR]);
                    log("Output value " + registers[outR] + " from R" + outR);
                    break;

                case ADR:
                    log("ADR");
                    int adrR = memory.getByte();
                    int adrAddr = memory.getInt();
                    addToPC(2);
                    if(isNotValidRegisters(adrR)) {
                        return;
                    }
                    registers[adrR] = adrAddr;
                    log("Address " + adrAddr + " stored in R" + adrR);
                    break;

                case B:
                    log("B");
                    int bAddr = memory.getInt();
                    setProgramCounter(bAddr);
                    log("Branching to address " + bAddr);
                    break;

                case BNE:
                    log("BNE");
                    int bneAddr = memory.getInt();
                    if (!zeroFlag) {
                        setProgramCounter(bneAddr);
                        log("Branching to address " + bneAddr + " because zero flag is not set");
                    } else {
                        addToPC(3);
                        log("Not branching because zero flag is set");
                    }
                    break;

                case END:
                    pcb.setRegisters(registers);
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
        int syscallID = memory.getInt();
        setKernelMode(true);
        switch (syscallID) {
            case 0:
                log("SWI: Printing register 0");
                System.out.println("Register 0: " + registers[0]);
                addToPC(1);
                break;
            case 1:
                log("SWI: Forking process");
                fork();
                break;
            case 2:
                log("SWI: Waiting for child process");
                waitProcess();
                break;
            default:
                logError("Invalid SWI call with ID: " + syscallID);
                break;
        }
        setKernelMode(false);
    }
}
