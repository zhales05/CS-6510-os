package os;

import os.queues.IReadyQueue;
import os.queues.MFQReadyQueue;
import os.queues.QueueId;
import os.util.Logging;
import os.util.MetricsTracker;
import util.Observer;
import vm.hardware.Clock;

import java.util.*;

/**
 * The Scheduler class is responsible for managing the queues and telling the OS when to run/swap processes.
 * It is also responsible for keeping track of metrics
 */
class Scheduler implements Logging, Observer {
    private static final Clock clock = Clock.getInstance();
    private final LinkedList<ProcessControlBlock> jobQueue = new LinkedList<>();
    private final LinkedList<ProcessControlBlock> ioQueue = new LinkedList<>();
    private final LinkedList<ProcessControlBlock> terminatedQueue = new LinkedList<>();
    private ProcessControlBlock currentProcess;

    private final Map<String, ProcessControlBlock> processMap = new HashMap<>();
    private IReadyQueue readyQueue;

    private final OperatingSystem parentOs;

    //metrics here for now
    List<MetricsTracker> metrics = new ArrayList<>();

    private Scheduler(OperatingSystem parentOs, IReadyQueue readyQueue) {
        this.parentOs = parentOs;
        setReadyQueue(readyQueue);
    }

    public Scheduler(OperatingSystem parentOs) {
        //this(parentOs, new RRReadyQueue(5));
        //this(parentOs, new FCFSReadyQueue());
        this(parentOs, new MFQReadyQueue(5, 10));
    }

    public void addToJobQueue(ProcessControlBlock pcb) {
        jobQueue.add(pcb);
        pcb.setStatus(ProcessStatus.NEW, QueueId.JOB_QUEUE);
        processMap.put(pcb.getFilePath(), pcb);
    }

    private void pushToBackOfJobQueue(ProcessControlBlock pcb) {
        jobQueue.add(pcb);
    }

    /**
     * This method is responsible for adding a process to the IO queue
     * this process will have been running every single time
     * @param pcb the process to add to the IO queue
     */
    public void addToIOQueue(ProcessControlBlock pcb) {
        log("Adding process " + pcb.getPid() + " to IO queue");
        pcb.setStatus(ProcessStatus.WAITING, QueueId.IO_QUEUE);
        ioQueue.add(pcb);

        transitionProcess();
    }

    //moved status change to ready queue
    private void addToReadyQueue(ProcessControlBlock pcb) {
        readyQueue.addProcess(pcb);
       // pcb.setStatus(ProcessStatus.READY, readyQueue.getQueueId());
    }

    public void addToTerminatedQueue(ProcessControlBlock pcb) {
        log("Adding process " + pcb.getPid() + " to terminated queue");
        pcb.setStatus(ProcessStatus.TERMINATED, QueueId.TERMINATED_QUEUE);

        if(pcb.equals(currentProcess)) {
            currentProcess = null;
        }

        terminatedQueue.add(pcb);
    }

    public ProcessControlBlock getProcess(String filePath) {
        return processMap.get(filePath);
    }

    public int getNewPid() {
        return getNumTotalProcesses() + 1;
    }

    public ProcessControlBlock getJob() {
        return jobQueue.poll();
    }


    private ProcessControlBlock getFromReadyQueue() {
        currentProcess = readyQueue.getNextProcess();
        return currentProcess;
    }

    public void processJobs() {
        MetricsTracker metricsTracker = new MetricsTracker();
        metrics.add(metricsTracker);

        while (!jobQueue.isEmpty()) {
            ProcessControlBlock pcb = getJob();
            if (pcb.getStartAfter() <= clock.getTime()) {
                pcb = parentOs.loadIntoMemory(pcb);
                //checking if error with load
                if (pcb == null) {
                    continue;
                }

                addToReadyQueue(pcb);

            } else {
                //put back in job queue
                pushToBackOfJobQueue(pcb);
                //ticking clock se we don't get stuck with a process that never starts
                clock.tick();
            }
        }

        while (!readyQueue.isEmpty()) {
            runProcess(getFromReadyQueue());
        }

        //print metrics here for now
        metricsTracker.calculateMetrics(terminatedQueue);
    }




    private void runProcess(ProcessControlBlock pcb) {
        pcb.setStatus(ProcessStatus.RUNNING, QueueId.RUNNING_QUEUE);
        readyQueue.resetQuantumCounter();
        parentOs.runProcess(pcb);
    }

    private ProcessControlBlock getFromIoQueue() {
        return ioQueue.poll();
    }

    public int getNumTotalProcesses() {
        return jobQueue.size() + readyQueue.
                size() + terminatedQueue.size() + ioQueue.size();
    }

    public ProcessControlBlock startChildProcess(ProcessControlBlock parent) {
       // addToIOQueue(parent);
        ProcessControlBlock pcb = new ProcessControlBlock(getNewPid(), "files/child.osx", 0);
        pcb = parentOs.loadIntoMemory(pcb);
        //skipping ready queue going straight to running
        runProcess(pcb);
       // getFromIoQueue(); //removing parent from io queue
        return pcb;
    }

    @Override
    public void clockTicked(int time) {
        if (currentProcess != null && readyQueue.incrementQuantumCounter()) {
            log("Quantum expired");
            //putting the current process back in the ready queue
            transitionProcess();
        }
    }

    /**
     * This method is responsible for transitioning the current process to the next process in the ready queue
     * this is only used if the there is a current process running
     */
    private void transitionProcess() {
        readyQueue.resetQuantumCounter();
        if (currentProcess != null) {
            //temporarily putting the current process back in the ready queue (should be done when it leaves IO queue)
            addToReadyQueue(currentProcess);
            //setting the new current process
            currentProcess = getFromReadyQueue();
            //running the new current process
            if(currentProcess != null) {
                currentProcess.setStatus(ProcessStatus.RUNNING, QueueId.RUNNING_QUEUE);
                parentOs.transitionProcess(currentProcess);
            }
        } else {
            logError("Tried to transition with no active running process");
        }
    }

    public void setReadyQueue(IReadyQueue readyQueue) {
        log("Setting ready queue to " + readyQueue.getClass().getSimpleName());
        this.readyQueue = readyQueue;
    }

}
