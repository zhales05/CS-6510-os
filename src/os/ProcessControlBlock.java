package os;

import os.queues.QueueId;
import os.util.Logging;
import os.util.ProcessExecutionBurst;
import vm.hardware.Clock;

import java.util.ArrayList;
import java.util.List;

public class ProcessControlBlock implements Logging {
    private static final int CPU_BURST_TOTAL = 5;
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

    //process specific metrics
    private final List<ProcessExecutionBurst> timeLine = new ArrayList<>();
    private int arrivalTime;
    private int completionTime;
    private int turnAroundTime;
    private int waitingTime = 0;
    private Integer responseTime;


    List<ProcessExecutionBurst> currentCPUBursts = new ArrayList<>();
    //if we currently have a start time and no end time we store it here
    private ProcessExecutionBurst currentTime;

    public ProcessControlBlock(int pid, String filePath, int startAfter) {
        this.pid = pid;
        this.filePath = filePath;
        this.startAfter = startAfter;
    }

    private void processStatusChange(ProcessStatus newStatus, QueueId queueId) {
        if (currentTime != null) {
            currentTime.setEnd();
            timeLine.add(currentTime);
            currentTime = null;
        }

        switch (newStatus) {
            case NEW:
                arrivalTime = clock.getTime();
                currentTime = new ProcessExecutionBurst(queueId);
                break;
            case RUNNING:
                currentTime = new ProcessExecutionBurst(queueId);

                if (currentCPUBursts.size() > CPU_BURST_TOTAL) {
                    currentCPUBursts.clear();
                }
                currentCPUBursts.add(currentTime);
                break;
            case READY:
                currentTime = new ProcessExecutionBurst(queueId);
                break;
            case WAITING:
                ProcessExecutionBurst peb = timeLine.getLast();
                //only set to finished if it was in a ready queue
                if (peb != null && QueueId.RUNNING_QUEUE.equals(queueId)) {
                    timeLine.getLast().setBurstFinished(true);
                }
                currentTime = new ProcessExecutionBurst(queueId);
                break;
            case TERMINATED:
                completionTime = clock.getTime();
                evaluateMetrics();
                printfTimeline();
                break;
        }
    }


    public void addChild(ProcessControlBlock pcb) {
        log("Adding child " + pcb.getPid() + " to " + pid);
        children.add(pcb);
    }

    public void printfTimeline() {
        StringBuilder sb = new StringBuilder("Process Timeline:\n");
        for (ProcessExecutionBurst pet : timeLine) {
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
        for (int i = arrivalTime; i < completionTime; i++) {
            sb.append(String.format("%4d", i));
        }
        sb.append("\n");

        StringBuilder job = new StringBuilder("Job:     ");
        StringBuilder ready = new StringBuilder("Ready:   ");
        StringBuilder running = new StringBuilder("Running: ");
        StringBuilder io = new StringBuilder("IO:      ");
        StringBuilder mfq1 = new StringBuilder("MFQ 1:   ");
        StringBuilder mfq2 = new StringBuilder("MFQ 2:   ");
        StringBuilder mfq3 = new StringBuilder("MFQ 3:   ");

        boolean usedMFQ1 = false;
        boolean usedMFQ2 = false;
        boolean usedMFQ3 = false;
        boolean usedReady = false;

        for (ProcessExecutionBurst pet : timeLine) {
            for (int i = pet.getStart(); i < pet.getEnd(); i++) {
                switch (pet.getQueueId()) {
                    case JOB_QUEUE:
                        job.append(String.format("%4s", "X"));
                        ready.append(String.format("%4s", ""));
                        running.append(String.format("%4s", ""));
                        io.append(String.format("%4s", ""));
                        mfq1.append(String.format("%4s", ""));
                        mfq2.append(String.format("%4s", ""));
                        mfq3.append(String.format("%4s", ""));

                        break;
                    case RUNNING_QUEUE:
                        job.append(String.format("%4s", ""));
                        ready.append(String.format("%4s", ""));
                        running.append(String.format("%4s", "X"));
                        io.append(String.format("%4s", ""));
                        mfq1.append(String.format("%4s", ""));
                        mfq2.append(String.format("%4s", ""));
                        mfq3.append(String.format("%4s", ""));

                        if (responseTime == null) {
                            responseTime = pet.getStart() - arrivalTime;
                        }

                        break;
                    case IO_QUEUE:
                        job.append(String.format("%4s", ""));
                        ready.append(String.format("%4s", ""));
                        running.append(String.format("%4s", ""));
                        io.append(String.format("%4s", "X"));
                        mfq1.append(String.format("%4s", ""));
                        mfq2.append(String.format("%4s", ""));
                        mfq3.append(String.format("%4s", ""));
                        break;
                    case RR_QUEUE, FCFS_QUEUE:
                        job.append(String.format("%4s", ""));
                        ready.append(String.format("%4s", "X"));
                        running.append(String.format("%4s", ""));
                        io.append(String.format("%4s", ""));
                        mfq1.append(String.format("%4s", ""));
                        mfq2.append(String.format("%4s", ""));
                        mfq3.append(String.format("%4s", ""));
                        waitingTime++;
                        usedReady = true;
                        break;
                    case MFQ_QUEUE_1:
                        job.append(String.format("%4s", ""));
                        ready.append(String.format("%4s", ""));
                        running.append(String.format("%4s", ""));
                        io.append(String.format("%4s", ""));
                        mfq1.append(String.format("%4s", "X"));
                        mfq2.append(String.format("%4s", ""));
                        mfq3.append(String.format("%4s", ""));
                        usedMFQ1 = true;
                        waitingTime++;
                        break;
                    case MFQ_QUEUE_2:
                        job.append(String.format("%4s", ""));
                        ready.append(String.format("%4s", ""));
                        running.append(String.format("%4s", ""));
                        io.append(String.format("%4s", ""));
                        mfq1.append(String.format("%4s", ""));
                        mfq2.append(String.format("%4s", "X"));
                        mfq3.append(String.format("%4s", ""));
                        usedMFQ2 = true;
                        waitingTime++;
                        break;
                    case MFQ_QUEUE_3:
                        job.append(String.format("%4s", ""));
                        ready.append(String.format("%4s", ""));
                        running.append(String.format("%4s", ""));
                        io.append(String.format("%4s", ""));
                        mfq1.append(String.format("%4s", ""));
                        mfq2.append(String.format("%4s", ""));
                        mfq3.append(String.format("%4s", "X"));
                        usedMFQ3 = true;
                        waitingTime++;
                        break;
                    case TERMINATED_QUEUE:
                        break;
                }
            }
        }

        sb.append(job).append("\n")
                .append(running).append("\n")
                .append(io).append("\n");

        if (usedReady) {
            sb.append(ready).append("\n");
        }
        if (usedMFQ1) {
            sb.append(mfq1).append("\n");
        }
        if (usedMFQ2) {
            sb.append(mfq2).append("\n");
        }
        if (usedMFQ3) {
            sb.append(mfq3).append("\n");
        }

        sb.append("Process ").append(pid).append(" Metrics:\n")
                .append("Turnaround Time: ").append(turnAroundTime).append("\n")
                .append("Waiting Time: ").append(waitingTime).append("\n")
                .append("Response Time: ").append(responseTime).append("\n");

        log(sb.toString());
    }


    public Double getBurstCompletionPercentage() {
        if (currentCPUBursts.size() != CPU_BURST_TOTAL) {
            return null;
        }
        int total = 0;
        int completed = 0;
        for (ProcessExecutionBurst pet : currentCPUBursts) {
            if (pet.isBurstFinished()) {
                completed++;
            }
            total++;
        }

        return (double) completed / total;
    }

    public QueueId getLastReadyQueue() {
        for (int i = timeLine.size() - 1; i >= 0; i--) {
            ProcessExecutionBurst burst = timeLine.get(i);
            if (QueueId.READY_QUEUES.contains(burst.getQueueId())) {
                return burst.getQueueId();
            }
        }
        return null;
    }


    public int getTurnAroundTime() {
        return turnAroundTime;
    }

    public void setTurnAroundTime(int turnAroundTime) {
        this.turnAroundTime = turnAroundTime;
    }

    public int getWaitingTime() {
        return waitingTime;
    }

    public void setWaitingTime(int waitingTime) {
        this.waitingTime = waitingTime;
    }

    public Integer getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(Integer responseTime) {
        this.responseTime = responseTime;
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

    public void setStatus(ProcessStatus status, QueueId queueId) {
        processStatusChange(status, queueId);
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
}
