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
    private String filePath;
    private final List<Integer> childPIDs = new ArrayList<>();
    private final int[] registers = new int[12];

    public ProcessControlBlock(int pid) {
        this.pid = pid;
        this.status = ProcessStatus.NEW;
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

    public void setStatus(ProcessStatus status) {
        this.status = status;
        log("Process " + pid + " is now " + status);
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

    public int[] getRegisters() {
        return registers;
    }

    public void setRegisters(int[] registers) {
        System.arraycopy(registers, 0, this.registers, 0, registers.length);
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public int getCodeStart() {
        return codeStart;
    }

    public void setCodeStart(int codeStart) {
        this.codeStart = codeStart;
    }

    public List<Integer> getChildPIDs() {
        return childPIDs;
    }

    public void addChildPID(Integer childPID) {
        childPIDs.add(childPID);
    }
}
