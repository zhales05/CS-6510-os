package os;

import os.queues.QueueIds;
import os.util.Logging;
import os.util.ProcessExecutionTime;
import vm.hardware.Clock;

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
    //arrival to ready queue should the timeline track this?
    private final int arrivalTime;
    private final List<ProcessExecutionTime> timeLine = new ArrayList<>();

    //if we currently have a start time and no end time we store it here
    private ProcessExecutionTime currentTime;

    public ProcessControlBlock(int pid, String filePath, int startAfter, int arrivalTime) {
        this.pid = pid;
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

    public void setStatus(ProcessStatus status, QueueIds queueId) {
        processStatusChange(status, queueId);
        this.status = status;
        log("Process " + pid + " is now " + status);
    }

    private void processStatusChange(ProcessStatus newStatus, QueueIds queueId) {
        if (currentTime != null) {
            currentTime.setEnd();
            timeLine.add(currentTime);
            currentTime = null;
        }

        switch (newStatus) {
            case NEW, RUNNING, WAITING, READY:
                currentTime =  new ProcessExecutionTime(queueId);
                break;
            case TERMINATED:
                printTimeline();
                break;
        }
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

    public void printTimeline() {
        StringBuilder sb = new StringBuilder("Process Timeline:\n");
        for (ProcessExecutionTime pet : timeLine) {
            sb.append("Queue: ").append(pet.getQueueId())
                    .append(", Start: ").append(pet.getStart())
                    .append(", End: ").append(pet.getEnd())
                    .append(", Execution Time: ").append(pet.getExecutionTime())
                    .append(" units\n");
        }
        System.out.println(sb.toString());
    }

    public void printdTimeline() {
        StringBuilder sb = new StringBuilder("Process Gantt Chart:\n");
        sb.append("Time: ");
        for (int i = 0; i <= Clock.getInstance().getTime(); i++) {
            sb.append(String.format("%4d", i));
        }
        sb.append("\n");

        for (ProcessExecutionTime pet : timeLine) {
            sb.append("Queue ").append(pet.getQueueId()).append(": ");
            for (int i = 0; i <= Clock.getInstance().getTime(); i++) {
                if (i >= pet.getStart() && i < pet.getEnd()) {
                    sb.append("####");
                } else {
                    sb.append("    ");
                }
            }
            sb.append("\n");
        }
        System.out.println(sb.toString());
    }

}
