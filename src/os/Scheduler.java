package os;

import os.util.Logging;

import java.util.LinkedList;

public class Scheduler implements Logging {
    private final LinkedList<String> jobQueue = new LinkedList<>();
    private final LinkedList<ProcessControlBlock> readyQueue = new LinkedList<>();

    private static Scheduler instance;

    private Scheduler() {
    }

    public static Scheduler getInstance() {
        if (instance == null) {
            instance = new Scheduler();
        }

        return instance;
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

    public void processJobs(OperatingSystem parentOs) {
        while (!jobQueue.isEmpty()) {
            //load
            ProcessControlBlock pcb = parentOs.prepareForReadyQueue(getJob());

            //checking if error with load
            if(pcb == null) {
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


}
