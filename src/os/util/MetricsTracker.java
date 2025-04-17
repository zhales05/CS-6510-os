package os.util;

import os.ProcessControlBlock;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;

public class MetricsTracker implements Logging {
    private double throughput = 0.0;
    private double waitingTime = 0.0;
    private double turnAroundTime = 0.0;
    private int responseTime = 0;
    private int quantum1;
    private int quantum2;
    private String filename;

    public void calculateMetrics(Collection<ProcessControlBlock> processes, int[] quantum, String filename) {
        this.quantum1 = quantum[0];
        this.quantum2 = quantum[1];
        this.filename = filename;

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
        log("Quantum 1: " + quantum1);
        log("Quantum 2: " + quantum2);


        try (FileWriter writer = new FileWriter("metrics_output.txt", true)) {
            writer.write("--------- System Metrics ---------\n");
            writer.write("file: " + filename + "\n");
            writer.write("Average Throughput: " + throughput + "\n");
            writer.write("Average Waiting Time: " + waitingTime + "\n");
            writer.write("Average Turnaround Time: " + turnAroundTime + "\n");
            writer.write("Average Response Time: " + responseTime + "\n");
            writer.write("Quantum 1: " + quantum1 + "\n");
            writer.write("Quantum 2: " + quantum2 + "\n");

        } catch (IOException e) {
            log("An error occurred while writing metrics to file: " + e.getMessage());
        }
    }

    public int getQuantum2() {
        return quantum2;
    }

    public double getWaitingTime() {
        return waitingTime;
    }

    public double getThroughput() {
        return throughput;
    }

    public double getTurnAroundTime() {
        return turnAroundTime;
    }

    public int getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(int responseTime) {
        this.responseTime = responseTime;
    }

    public int getQuantum1() {
        return quantum1;
    }
}
