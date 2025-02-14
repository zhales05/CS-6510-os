package os;

import os.util.Logging;
import vm.hardware.Memory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

class Scheduler implements Logging {
    private final LinkedList<String> jobQueue = new LinkedList<>();
    private final LinkedList<ProcessControlBlock> readyQueue = new LinkedList<>();
    private final LinkedList<ProcessControlBlock> terminatedQueue = new LinkedList<>();
    private final Map<String, ProcessControlBlock> activePcbs = new HashMap<>();


    private OperatingSystem parentOs;

    public Scheduler(OperatingSystem parentOs) {
        this.parentOs = parentOs;
    }

    public void addJob(String fileName) {
        jobQueue.add(fileName);
    }

    public String getJob() {
        return jobQueue.poll();
    }

    private void addToReadyQueue(ProcessControlBlock pcb) {
        log("Adding process " + pcb.getPid() + " to ready queue");
        readyQueue.add(pcb);
    }

    private ProcessControlBlock getFromReadyQueue() {
        return readyQueue.poll();
    }

    public void processJobs() {
        while (!jobQueue.isEmpty()) {
            //load
            ProcessControlBlock pcb = parentOs.prepareForReadyQueue(getJob());

            //checking if error with load
            if (pcb == null) {
                continue;
            }
            //put in ready queue
            pcb.setStatus(ProcessStatus.READY);
            addToReadyQueue(pcb);
            // right now we are just going to run as soon as we add to ready queue.
            // this will change in the future
            parentOs.runProcess(getFromReadyQueue());
        }
    }


    public void addToTerminatedQueue(ProcessControlBlock pcb) {
        log("Adding process " + pcb.getPid() + " to terminated queue");
        pcb.setStatus(ProcessStatus.TERMINATED);
        terminatedQueue.add(pcb);
        //might need to do this somewhere else later but this should clean it all up for now
        parentOs.removeProcess(pcb);
    }

    public Integer startChildProcess() {
        //TODO pass in asm and have it become a OSX
        ProcessControlBlock pcb = parentOs.prepareForReadyQueue("files/child.osx");
        //skipping ready queue going straight to running
        parentOs.runProcess(pcb);
        return pcb.getPid();
    }

}
