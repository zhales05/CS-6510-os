package os;

import os.util.Logging;

import java.util.ArrayList;
import java.util.List;

public class ProcessControlBlock implements Logging {
    private final int pid;
    private ProcessStatus status;
    private int programSize;
    private int programStart;
    private int codeStart;
    private final int startAfter;
    private final String filePath;
    private final List<ProcessControlBlock> children = new ArrayList<>();
    private final int[] registers = new int[12];

    //metrics
    private int arrivalTime;
    private int completionTime;
    private final List<int[]> executionTimes = new ArrayList<>();


    public ProcessControlBlock(int pid, String filePath, int startAfter, int arrivalTime) {
        this.pid = pid;
        this.status = ProcessStatus.NEW;
        this.filePath = filePath;
        this.startAfter = startAfter;
        this.arrivalTime = arrivalTime;
    }

    public int getProgramStart() {
        return programStart;
    }

    public void setProgramStart(int programStart) {
        this.programStart = programStart;
    }

    public int getPid() {
        return pid;
    }

    public ProcessStatus getStatus() {
        return status;
    }

    public void setStatus(ProcessStatus status, int time) {
        this.status = status;
        log("Process " + pid + " is now " + status);
        completionTime = time;
    }

    public int getProgramSize() {
        return programSize;
    }

    public void setProgramSize(int programSize) {
        this.programSize = programSize;
    }

    public int getPc() {
        return registers[11];
    }

    public void setPc(int pc) {
        registers[11] = pc;
    }

    public int getStartAfter() {
        return startAfter;
    }

    public int[] getRegisters() {
        return registers;
    }

    public void setRegisters(int[] registers) {
        System.arraycopy(registers, 0, this.registers, 0, registers.length);
    }

    public String getFilePath() {
        return filePath;
    }


    public int getCodeStart() {
        return codeStart;
    }

    public void setCodeStart(int codeStart) {
        this.codeStart = codeStart;
    }

    public void addChild(ProcessControlBlock pcb) {
        log("Adding child " + pcb.getPid() + " to " + pid);
        children.add(pcb);
    }

}
