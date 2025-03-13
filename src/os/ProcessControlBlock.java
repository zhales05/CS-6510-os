package os;

import os.queues.QueueIds;
import os.util.Logging;
import os.util.ProcessExecutionTime;
import vm.hardware.Clock;

import java.util.ArrayList;
import java.util.List;

public class ProcessControlBlock implements Logging {
    Clock clock = Clock.getInstance();
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
    private final List<ProcessExecutionTime> timeLine = new ArrayList<>();
    private int arrivalTime;
    private int completionTime;
    private int turnAroundTime;
    private int waitingTime = 0;
    private Integer responseTime;

    //if we currently have a start time and no end time we store it here
    private ProcessExecutionTime currentTime;

    public ProcessControlBlock(int pid, String filePath, int startAfter) {
        this.pid = pid;
        this.filePath = filePath;
        this.startAfter = startAfter;
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
            case NEW:
                arrivalTime = clock.getTime();
                currentTime = new ProcessExecutionTime(queueId);
                break;
            case RUNNING, WAITING, READY:
                currentTime = new ProcessExecutionTime(queueId);
                break;
            case TERMINATED:
                completionTime = clock.getTime();
                evaluateMetrics();
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

    public void printfTimeline() {
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

    public void evaluateMetrics() {
        turnAroundTime = completionTime - arrivalTime;

        StringBuilder sb = new StringBuilder("Process " + pid + " Gantt Chart:\n");
        sb.append("Time:    ");
        for (int i = timeLine.getFirst().getStart(); i < Clock.getInstance().getTime(); i++) {
            sb.append(String.format("%4d", i));
        }
        sb.append("\n");

        StringBuilder job = new StringBuilder("Job:     ");
        StringBuilder ready = new StringBuilder("Ready:   ");
        StringBuilder running = new StringBuilder("Running: ");
        StringBuilder io = new StringBuilder("IO:      ");

        for (ProcessExecutionTime pet : timeLine) {
            for (int i = pet.getStart(); i < pet.getEnd(); i++) {
                switch (pet.getQueueId()) {
                    case JOB_QUEUE:
                        job.append(String.format("%4s", "X"));
                        ready.append(String.format("%4s", ""));
                        running.append(String.format("%4s", ""));
                        io.append(String.format("%4s", ""));
                        break;
                    case RUNNING_QUEUE:
                        job.append(String.format("%4s", ""));
                        ready.append(String.format("%4s", ""));
                        running.append(String.format("%4s", "X"));
                        io.append(String.format("%4s", ""));

                        if (responseTime == null) {
                            responseTime = pet.getStart() - arrivalTime;
                        }

                        break;
                    case IO_QUEUE:
                        job.append(String.format("%4s", ""));
                        ready.append(String.format("%4s", ""));
                        running.append(String.format("%4s", ""));
                        io.append(String.format("%4s", "X"));
                        break;
                    case RR_QUEUE:
                        job.append(String.format("%4s", ""));
                        ready.append(String.format("%4s", "X"));
                        running.append(String.format("%4s", ""));
                        io.append(String.format("%4s", ""));

                        waitingTime++;

                        break;
                    case TERMINATED_QUEUE:
                        break;
                }
            }
        }

        sb.append(job).append("\n")
                .append(ready).append("\n")
                .append(running).append("\n")
                .append(io).append("\n")
                .append("Turnaround Time: ").append(turnAroundTime).append("\n")
                .append("Waiting Time: ").append(waitingTime).append("\n")
                .append("Response Time: ").append(responseTime).append("\n");

        log(sb.toString());
    }

}
