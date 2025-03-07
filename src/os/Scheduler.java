package os;

import os.queues.FCFSReadyQueue;
import os.queues.IReadyQueue;
import os.queues.RRReadyQueue;
import os.util.Logging;
import os.util.MetricsTracker;
import util.Observer;
import vm.hardware.Clock;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

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
    private MetricsTracker metrics = new MetricsTracker();
    private Integer clockStartTime;

    private Scheduler(OperatingSystem parentOs, IReadyQueue readyQueue) {
        this.parentOs = parentOs;
        setReadyQueue(readyQueue);
    }

    public Scheduler(OperatingSystem parentOs) {
        this(parentOs, new RRReadyQueue(5));
    }

    public void addToJobQueue(ProcessControlBlock pcb) {
        jobQueue.add(pcb);
        processMap.put(pcb.getFilePath(), pcb);
    }

    /**
     * This method is responsible for adding a process to the IO queue
     * this process will have been running every single time
     * @param pcb the process to add to the IO queue
     */
    public void addToIOQueue(ProcessControlBlock pcb) {
        log("Adding process " + pcb.getPid() + " to IO queue");
        pcb.setStatus(ProcessStatus.WAITING, clock.getTime());
        ioQueue.add(pcb);

        transitionProcess();
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

    private void addToReadyQueue(ProcessControlBlock pcb) {
        pcb.setStatus(ProcessStatus.READY, Clock.getInstance().getTime());
        readyQueue.addProcess(pcb);
    }


    private ProcessControlBlock getFromReadyQueue() {
        currentProcess = readyQueue.getNextProcess();
        return currentProcess;
    }

    public void processJobs() {
        while (!jobQueue.isEmpty()) {
            ProcessControlBlock pcb = getJob();
            if (pcb.getStartAfter() <= clock.getTime()) {
                //load
                pcb = parentOs.prepareForReadyQueue(pcb);
                //checking if error with load
                if (pcb == null) {
                    continue;
                }
                //put in ready queue
                addToReadyQueue(pcb);

                //if I move a pcb into the ready queue we need to log the clock start time
                // however if the clock start time is not null it has already been set
                if (clockStartTime == null) {
                    clockStartTime = clock.getTime();
                }
            } else {
                //put back in job queue
                jobQueue.add(pcb);
                //ticking clock se we don't get stuck with a process that never starts
                clock.tick();
            }
        }

        while (!readyQueue.isEmpty()) {
            readyQueue.resetQuantumCounter();
            parentOs.runProcess(getFromReadyQueue());
        }

        //finalize metrics
        metrics.addThroughput(readyQueue.getQuantum(), terminatedQueue.size(), clockStartTime, clock.getTime());

        //reset clock start time so we can do it all again
        clockStartTime = null;

        //print metrics here for now
        log(metrics.toString());
    }


    public void addToTerminatedQueue(ProcessControlBlock pcb) {
        log("Adding process " + pcb.getPid() + " to terminated queue");
        pcb.setStatus(ProcessStatus.TERMINATED, clock.getTime());

        if(pcb.equals(currentProcess)) {
            currentProcess = null;
        }

        terminatedQueue.add(pcb);
        //might need to do this somewhere else later but this should clean it all up for now
        //parentOs.removeProcess(pcb);
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
        ProcessControlBlock pcb = new ProcessControlBlock(getNewPid(), "files/child.osx", 0, clock.getTime());
        pcb = parentOs.prepareForReadyQueue(pcb);
        //skipping ready queue going straight to running
        parentOs.runProcess(pcb);
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
        if (currentProcess != null) {
            addToReadyQueue(currentProcess);
            //setting the new current process
            currentProcess = getFromReadyQueue();
            readyQueue.resetQuantumCounter();
            //running the new current process
            parentOs.transitionProcess(currentProcess);
        } else {
            logError("Tried to transition with no active running process");
        }
    }

    public void setReadyQueue(IReadyQueue readyQueue) {
        log("Setting ready queue to " + readyQueue.getClass().getSimpleName());
        this.readyQueue = readyQueue;
    }


}
