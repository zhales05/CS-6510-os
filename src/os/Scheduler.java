package os;

import os.util.Logging;
import util.Observer;
import vm.hardware.Clock;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

class Scheduler implements Logging, Observer {
    private static final Clock clock = Clock.getInstance();
    private final LinkedList<ProcessControlBlock> jobQueue = new LinkedList<>();
    private final LinkedList<ProcessControlBlock> ioQueue = new LinkedList<>();
    private final LinkedList<ProcessControlBlock> terminatedQueue = new LinkedList<>();

    private final Map<String, ProcessControlBlock> processMap = new HashMap<>();
    private final IReadyQueue readyQueue;

    private final OperatingSystem parentOs;

    public Scheduler(OperatingSystem parentOs, IReadyQueue readyQueue) {
        this.parentOs = parentOs;
        this.readyQueue = readyQueue;
    }

    public void addToJobQueue(ProcessControlBlock pcb) {
        jobQueue.add(pcb);
        processMap.put(pcb.getFilePath(), pcb);
    }

    public void addToIOQueue(ProcessControlBlock pcb) {
        log("Adding process " + pcb.getPid() + " to IO queue");
        pcb.setStatus(ProcessStatus.WAITING);
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
       readyQueue.addProcess(pcb);
    }


    private ProcessControlBlock getFromReadyQueue() {
        return readyQueue.getNextProcess();
    }

    public void processJobs() {
        while (!jobQueue.isEmpty()) {
            ProcessControlBlock pcb = getJob();
            if (pcb.getClockStartTime() <= clock.getTime()) {
                //load
                pcb = parentOs.prepareForReadyQueue(pcb);
                //checking if error with load
                if (pcb == null) {
                    continue;
                }
                //put in ready queue
                addToReadyQueue(pcb);
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
    }


    public void addToTerminatedQueue(ProcessControlBlock pcb) {
        log("Adding process " + pcb.getPid() + " to terminated queue");
        pcb.setStatus(ProcessStatus.TERMINATED);
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
        ProcessControlBlock pcb = new ProcessControlBlock(getNewPid(), "files/child.osx", 0);
        pcb = parentOs.prepareForReadyQueue(pcb);
        //skipping ready queue going straight to running
        parentOs.runProcess(pcb);
        getFromIoQueue(); //removing parent from io queue
        return pcb;
    }

    @Override
    public void clockTicked(int time) {
        //update queues here (maybe not until milestone 3 though)
    }
}
