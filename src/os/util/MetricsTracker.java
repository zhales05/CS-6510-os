package os.util;

import os.ProcessControlBlock;

import java.util.Collection;

public class MetricsTracker implements Logging {
    private double throughput;
    private double waitingTime;
    private double turnAroundTime;
    private double responseTime;

    public void calculateMetrics(Collection<ProcessControlBlock> processes) {
        for (ProcessControlBlock process : processes) {
            waitingTime += process.getWaitingTime();
            turnAroundTime += process.getTurnAroundTime();
            responseTime += process.getResponseTime();
        }

        int numProcesses = processes.size();

        throughput = numProcesses / turnAroundTime;
        waitingTime /= numProcesses;
        turnAroundTime /= numProcesses;
        responseTime /= numProcesses;

        printMetrics();
    }

    private void printMetrics() {
        log("--------- System Metrics ---------");
        log("Average Throughput: " + throughput);
        log("Average Waiting Time: " + waitingTime);
        log("Average Turnaround Time: " + turnAroundTime);
        log("Average Response Time: " + responseTime);
    }

}
