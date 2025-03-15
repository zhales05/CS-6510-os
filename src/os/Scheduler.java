package os;

import os.queues.*;
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
    private final IOQueue ioQueue = new IOQueue();
    private final LinkedList<ProcessControlBlock> terminatedQueue = new LinkedList<>();
    private ProcessControlBlock currentProcess;

    private final Map<String, ProcessControlBlock> processMap = new HashMap<>();
    private IReadyQueue readyQueue;

    private final OperatingSystem parentOs;

    // Metrics tracker
    private MetricsTracker metricsTracker = new MetricsTracker();
    private String currentSchedulerType = "unknown";

    private Scheduler(OperatingSystem parentOs, IReadyQueue readyQueue) {
        this.parentOs = parentOs;
        setReadyQueue(readyQueue);
    }

    public Scheduler(OperatingSystem parentOs) {
        //this(parentOs, new RRReadyQueue(5));
        //this(parentOs, new FCFSReadyQueue());
        this(parentOs, new MFQReadyQueue(5, 10));
        currentSchedulerType = "mfq";
        metricsTracker.setSchedulerInfo(currentSchedulerType, 5, 10);
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
     *
     * @param pcb the process to add to the IO queue
     */
    public void addToIOQueue(ProcessControlBlock pcb) {
        ioQueue.add(pcb);
        transitionProcess();
    }

    //moved status change to ready queue
    private void addToReadyQueue(ProcessControlBlock pcb) {
        readyQueue.addProcess(pcb);
    }

    public void addToTerminatedQueue(ProcessControlBlock pcb) {
        terminatedQueue.add(pcb);
        pcb.setStatus(ProcessStatus.TERMINATED, QueueId.TERMINATED_QUEUE);
        pcb.evaluateMetrics();
        
        // CPU burst data is now collected directly in the MetricsCollector
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

        runThroughReadyQueue();

        // Calculate and print metrics
        metricsTracker.calculateMetrics(terminatedQueue);
    }

    private void runThroughReadyQueue() {
        while (!readyQueue.isEmpty() || !ioQueue.isEmpty()) {
            ProcessControlBlock pcb = getFromReadyQueue();
            if(pcb == null){
                clock.tick();
                continue;
            }

            runProcess(pcb);
        }
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
        ProcessControlBlock pcb = new ProcessControlBlock(getNewPid(), "files/child.osx", 0);
        pcb = parentOs.loadIntoMemory(pcb);
        runProcess(pcb);
        return pcb;
    }

    @Override
    public void clockTicked(int time) {
        while (ioQueue.isReadyToLeave()) {
            ProcessControlBlock pcb = getFromIoQueue();
            addToReadyQueue(pcb);
        }

        if (currentProcess != null && readyQueue.incrementQuantumCounter()) {
            log("Quantum expired");
            //putting the current process back in the ready queue because it's a quantum
            addToReadyQueue(currentProcess);
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
            //setting the new current process
            currentProcess = getFromReadyQueue();
            //running the new current process
            if (currentProcess != null) {
                currentProcess.setStatus(ProcessStatus.RUNNING, QueueId.RUNNING_QUEUE);
                parentOs.transitionProcess(currentProcess);
            } else {
                //nothing in ready queue, probably stuck in IO
                parentOs.stopProcess();
                currentProcess = null;
                clock.tick();
            }
        }
        //if current process == null then we're probably waiting on io and the clock ticked
    }

    public void setReadyQueue(IReadyQueue readyQueue) {
        this.readyQueue = readyQueue;
        
        // Update scheduler type for metrics
        if (readyQueue instanceof FCFSReadyQueue) {
            currentSchedulerType = "fcfs";
            metricsTracker.setSchedulerInfo(currentSchedulerType, 0);
        } else if (readyQueue instanceof RRReadyQueue) {
            currentSchedulerType = "rr";
            int quantum = ((RRReadyQueue) readyQueue).getQuantum();
            metricsTracker.setSchedulerInfo(currentSchedulerType, quantum);
        } else if (readyQueue instanceof MFQReadyQueue) {
            currentSchedulerType = "mfq";
            // For MFQ, we'll use default values since we don't have direct access to the quantum values
            int quantum1 = 5; // Default value
            int quantum2 = 10; // Default value
            metricsTracker.setSchedulerInfo(currentSchedulerType, quantum1, quantum2);
        }
    }
}
