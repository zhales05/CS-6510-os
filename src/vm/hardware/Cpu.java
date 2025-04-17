package vm.hardware;

import os.OperatingSystem;
import os.ProcessControlBlock;
import os.util.Logging;

import java.util.Random;


public class Cpu implements Logging {
    private static Cpu instance;
    private final Memory memory = Memory.getInstance();
    private final SharedMemoryManager sharedMemoryManager = SharedMemoryManager.getInstance();
    private final SemaphoreManager semaphoreManager = SemaphoreManager.getInstance();
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

    static final int SWI_PRINT_R0 = 0;
    static final int SWI_PRINT_R1 = 1;
    static final int SWI_VFORK = 2;
    static final int SWI_WAIT = 3;
    static final int SWI_IO = 4;
    static final int SWI_SHM_OPEN = 5;
    static final int SWI_SHM_UNLINK = 6;
    static final int SWI_SEM_INIT = 7;
    static final int SWI_SEM_WAIT = 8;
    static final int SWI_SEM_SIGNAL = 9;

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
            case SWI_PRINT_R0:
                log("Printing register 0");
                System.out.println("Register 0: " + registers[0]);
                break;
            case SWI_PRINT_R1:
                log("Printing register 1");
                System.out.println("Register 1: " + registers[1]);
                break;
            case SWI_VFORK:
                log("vfork");
                startChildProcess(os, pcb);
                break;
            case SWI_WAIT:
                log("wait");
                Random random = new Random();
                int randomTicks = random.nextInt(20) + 1;
                log("Waiting for " + randomTicks + " ticks");
                Clock.getInstance().tick(randomTicks);
                break;
            case SWI_IO:
                log("io");
                os.addToIOQueue(currentPcb);
                break;
            case SWI_SHM_OPEN:
                handleShmOpen();
                break;
            case SWI_SHM_UNLINK:
                handleShmUnlink();
                break;
            case SWI_SEM_INIT:
                handleSemInit();
                break;
            case SWI_SEM_WAIT:
                handleSemWait(os);
                break;
            case SWI_SEM_SIGNAL:
                handleSemSignal(os);
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

    private void handleShmOpen() {
        log("shm_open");
        
        int namePtr = registers[0];
        int mode = registers[1];
        int size = registers[2];
        
        StringBuilder nameBuilder = new StringBuilder();
        int currentPtr = namePtr;
        byte currentByte;
        
        int savedPC = getProgramCounter();
        setProgramCounter(currentPtr);
        
        for (int i = 0; i < 64; i++) {
            currentByte = memory.getByte();
            if (currentByte == 0) {
                break;
            }
            nameBuilder.append((char) currentByte);
        }
        
        setProgramCounter(savedPC);
        
        String name = nameBuilder.toString();
        log("SHM name: " + name + ", mode: " + mode + ", size: " + size);
        
        int result = sharedMemoryManager.shmOpen(name, mode, size);
        
        registers[0] = result;
        log("SHM address: " + result);
    }
    
    private void handleShmUnlink() {
        log("shm_unlink");
        
        int namePtr = registers[0];
        
        StringBuilder nameBuilder = new StringBuilder();
        int currentPtr = namePtr;
        
        int savedPC = getProgramCounter();
        setProgramCounter(currentPtr);
        
        for (int i = 0; i < 64; i++) {
            byte currentByte = memory.getByte();
            if (currentByte == 0) {
                break;
            }
            nameBuilder.append((char) currentByte);
        }
        
        setProgramCounter(savedPC);
        
        String name = nameBuilder.toString();
        log("SHM name: " + name);
        
        boolean result = sharedMemoryManager.shmUnlink(name);
        
        registers[0] = result ? 1 : 0;
        log("SHM unlink result: " + result);
    }

    private void handleSemInit() {
        log("sem_init");
        
        int namePtr = registers[0];
        int initialValue = registers[1];
        
        String name = readStringFromMemory(namePtr);
        log("SEM name: " + name + ", initial value: " + initialValue);
        
        boolean result = semaphoreManager.semInit(name, initialValue);
        
        registers[0] = result ? 1 : 0;
        log("SEM init result: " + result);
    }
    
    private void handleSemWait(OperatingSystem os) {
        log("sem_wait");
        
        int namePtr = registers[0];
        
        String name = readStringFromMemory(namePtr);
        log("SEM wait name: " + name);
        
        boolean result = semaphoreManager.semWait(name, currentPcb);
        
        registers[0] = result ? 1 : 0;
        log("SEM wait result: " + result);
        
        if (!result) {
            os.blockProcess(currentPcb);
        }
    }
    
    private void handleSemSignal(OperatingSystem os) {
        log("sem_signal");
        
        int namePtr = registers[0];
        
        String name = readStringFromMemory(namePtr);
        log("SEM signal name: " + name);
        
        ProcessControlBlock unblocked = semaphoreManager.semSignal(name);
        
        if (unblocked != null) {
            os.unblockProcess(unblocked);
        }
        
        registers[0] = 1;
        log("SEM signal completed");
    }
    
    private String readStringFromMemory(int address) {
        StringBuilder builder = new StringBuilder();
        int savedPC = getProgramCounter();
        setProgramCounter(address);
        
        for (int i = 0; i < 64; i++) {
            byte b = memory.getByte();
            if (b == 0) {
                break;
            }
            builder.append((char) b);
        }
        
        setProgramCounter(savedPC);
        return builder.toString();
    }
}
