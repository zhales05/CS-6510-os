package os;


import os.queues.*;
import os.util.Logging;
import os.util.MetricsTracker;
import os.util.ProcessStatus;
import os.util.SystemGanttChart;
import util.Observer;
import vm.hardware.Clock;

import java.util.*;
import java.util.List;

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
    private final List<ProcessControlBlock> currentProcesses = new ArrayList<>();
    private IReadyQueue readyQueue;

    private final OperatingSystem parentOs;

    //metrics here for now
    List<MetricsTracker> metrics = new ArrayList<>();

    private Scheduler(OperatingSystem parentOs, IReadyQueue readyQueue) {
        this.parentOs = parentOs;
        setReadyQueue(readyQueue);
    }

    public Scheduler(OperatingSystem parentOs) {
        this(parentOs, new RRReadyQueue(5));
        //this(parentOs, new MFQReadyQueue(5, 10));
    }

    public void addToJobQueue(ProcessControlBlock pcb) {
        jobQueue.add(pcb);
        pcb.setStatus(ProcessStatus.NEW, QueueId.JOB_QUEUE);
        processMap.put(pcb.getFilePath(), pcb);
        currentProcesses.add(pcb);
    }

    private void pushToBackOfJobQueue(ProcessControlBlock pcb) {
        jobQueue.add(pcb);
    }

    public void addToIOQueue(ProcessControlBlock pcb) {
        ioQueue.add(pcb);
        transitionProcess();
    }

    //moved status change to ready queue
    private void addToReadyQueue(ProcessControlBlock pcb) {
        readyQueue.addProcess(pcb);
    }

    public void addToTerminatedQueue(ProcessControlBlock pcb) {
        log("Adding process " + pcb.getPid() + " to terminated queue");
        pcb.setStatus(ProcessStatus.TERMINATED, QueueId.TERMINATED_QUEUE);

        if (pcb.equals(currentProcess)) {
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

    public void processJobsForReadyQueue() {
        MetricsTracker metricsTracker = new MetricsTracker();
        metrics.add(metricsTracker);

        while (!jobQueue.isEmpty()) {
            ProcessControlBlock pcb = getJob();
            if (pcb.getStartAfter() <= clock.getTime()) {
                pcb = loadIntoMemory(pcb);
                //checking if error with load
                if (pcb == null) {
                    continue;
                }

                addToReadyQueue(pcb);

            } else {
                //put back in job queue
                pushToBackOfJobQueue(pcb);
                //ticking clock so we don't get stuck with a process that never starts
                clock.tick();
            }
        }
        //print metrics here for now
       // metricsTracker.calculateMetrics(currentProcesses, readyQueue.getQuantum(), currentProcesses.getLast().getFilePath());
    }

    public void loadJobs(){
        for (ProcessControlBlock pcb : jobQueue) {
            loadIntoMemory(pcb);
        }
    }

    public void run(String[] inputs) {
        Set<String> inputSet = new HashSet<>(Arrays.asList(inputs));  // faster lookup
        Iterator<ProcessControlBlock> iterator = jobQueue.iterator();

        while (iterator.hasNext()) {
            ProcessControlBlock pcb = iterator.next();
            if (inputSet.contains(pcb.getFilePath())) {
                addToReadyQueue(pcb);
                iterator.remove();
            }
        }
    }

    public ProcessControlBlock loadIntoMemory(ProcessControlBlock pcb) {
        return parentOs.loadIntoMemory(pcb);
    }

    public void runThroughReadyQueue() {
        while (!readyQueue.isEmpty() || !ioQueue.isEmpty()) {
            ProcessControlBlock pcb = getFromReadyQueue();
            if (pcb == null) {
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

    private void transitionProcess() {
        readyQueue.resetQuantumCounter();
        if (currentProcess != null) {
            parentOs.transitionProcess();
        }
    }


    public void setReadyQueue(IReadyQueue readyQueue) {
        log("Setting ready queue to " + readyQueue.getClass().getSimpleName());
        this.readyQueue = readyQueue;
    }

    public void clearCurrentProcesses() {
        currentProcesses.clear();
    }

    public void systemGanttChart() {
        SystemGanttChart.makeChart(currentProcesses);
        System.out.println("Total Page Faults: " + totalPageFaults());
    }

    private int totalPageFaults() {
        int pageFaults = 0;
        for (ProcessControlBlock pcb : currentProcesses) {
            pageFaults += pcb.getPageFaults();
        }
        return pageFaults;
    }

    public List<ProcessControlBlock> getCurrentProcesses(){
        return currentProcesses;
    }


}
