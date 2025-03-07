package os;

import os.util.Logging;
import os.util.MetricsTracker;
import util.Observer;
import vm.hardware.Clock;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * The Scheduler class is responsible for managing the queues and telling the OS when to run/swap processes.
 *  It is also responsible for keeping track of metrics
 */
class Scheduler implements Logging, Observer {
    private static final Clock clock = Clock.getInstance();
    private final LinkedList<ProcessControlBlock> jobQueue = new LinkedList<>();
    private final LinkedList<ProcessControlBlock> ioQueue = new LinkedList<>();
    private final LinkedList<ProcessControlBlock> terminatedQueue = new LinkedList<>();

    private final Map<String, ProcessControlBlock> processMap = new HashMap<>();
    private IReadyQueue readyQueue;

    private final OperatingSystem parentOs;

    //metrics here for now
    private MetricsTracker metrics = new MetricsTracker();
    private Integer clockStartTime;

    private Scheduler(OperatingSystem parentOs, IReadyQueue readyQueue) {
        this.parentOs = parentOs;
        this.readyQueue = readyQueue;
    }

    public Scheduler(OperatingSystem parentOs) {
        this(parentOs, new FCFSReadyQueue());
    }

    public void addToJobQueue(ProcessControlBlock pcb) {
        jobQueue.add(pcb);
        processMap.put(pcb.getFilePath(), pcb);
    }

    public void addToIOQueue(ProcessControlBlock pcb) {
        log("Adding process " + pcb.getPid() + " to IO queue");
        pcb.setStatus(ProcessStatus.WAITING, clock.getTime());
        ioQueue.add(pcb);
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
        return readyQueue.getNextProcess();
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
                if(clockStartTime == null) {
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
        addToIOQueue(parent);
        ProcessControlBlock pcb = new ProcessControlBlock(getNewPid(), "files/child.osx", 0, clock.getTime());
        pcb = parentOs.prepareForReadyQueue(pcb);
        //skipping ready queue going straight to running
        parentOs.runProcess(pcb);
        getFromIoQueue(); //removing parent from io queue
        return pcb;
    }

    @Override
    public void clockTicked(int time) {
        if(readyQueue.incrementQuantumCounter()){
        }
        //if true call os.transition or something
        // goes to cpu and transitions the process out and the new one in
    }

    public void setReadyQueue(IReadyQueue readyQueue) {
        this.readyQueue = readyQueue;
    }


}
