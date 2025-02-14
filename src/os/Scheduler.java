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
    private final LinkedList<ProcessControlBlock> readyQueue = new LinkedList<>();
    private final LinkedList<ProcessControlBlock> terminatedQueue = new LinkedList<>();

    private final Map<String, ProcessControlBlock> processMap = new HashMap<>();

    private final OperatingSystem parentOs;

    public Scheduler(OperatingSystem parentOs) {
        this.parentOs = parentOs;
    }

    public void addToJobQueue(ProcessControlBlock pcb) {
        jobQueue.add(pcb);
        processMap.put(pcb.getFilePath(), pcb);
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
        log("Adding process " + pcb.getPid() + " to ready queue");
        pcb.setStatus(ProcessStatus.READY);
        readyQueue.add(pcb);
    }

    private ProcessControlBlock getFromReadyQueue() {
        return readyQueue.poll();
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
                // right now we are just going to run as soon as we add to ready queue.
                // this will change in the future
                parentOs.runProcess(getFromReadyQueue());
            } else {
                //put back in job queue
                jobQueue.add(pcb);
                //ticking clock se we don't get stuck with a process that never starts
                clock.tick();
            }
        }
    }


    public void addToTerminatedQueue(ProcessControlBlock pcb) {
        log("Adding process " + pcb.getPid() + " to terminated queue");
        pcb.setStatus(ProcessStatus.TERMINATED);
        terminatedQueue.add(pcb);
        //might need to do this somewhere else later but this should clean it all up for now
        parentOs.removeProcess(pcb);
    }

    public int getNumTotalProcesses() {
        return jobQueue.size() + readyQueue.size() + terminatedQueue.size();
    }

    public Integer startChildProcess() {
        ProcessControlBlock pcb = new ProcessControlBlock(getNewPid(), "files/child.osx", 0);
        pcb = parentOs.prepareForReadyQueue(pcb);
        //skipping ready queue going straight to running
        parentOs.runProcess(pcb);
        return pcb.getPid();
    }

    @Override
    public void clockTicked(int time) {
        //update queues here (maybe not until milestone 3 though)
    }
}
