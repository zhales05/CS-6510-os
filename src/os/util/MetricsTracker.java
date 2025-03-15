package os.util;

import os.ProcessControlBlock;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

public class MetricsTracker implements Logging {
    private static final String OUTPUT_DIRECTORY = "metrics";
    
    private String schedulerType = "unknown";
    private int quantum = 0;
    private int quantum2 = 0;
    
    private double throughput;
    private double waitingTime;
    private double turnAroundTime;
    private double responseTime;
    private Collection<ProcessControlBlock> processes;

    public MetricsTracker() {
        // Create metrics directory if it doesn't exist
        try {
            Path path = Paths.get(OUTPUT_DIRECTORY);
            if (!Files.exists(path)) {
                Files.createDirectory(path);
                log("Created metrics directory: " + path.toAbsolutePath());
            }
        } catch (IOException e) {
            logError("Failed to create metrics directory: " + e.getMessage());
        }
    }
    
    public void setSchedulerInfo(String type, int quantum) {
        this.schedulerType = type;
        this.quantum = quantum;
    }
    
    public void setSchedulerInfo(String type, int quantum1, int quantum2) {
        this.schedulerType = type;
        this.quantum = quantum1;
        this.quantum2 = quantum2;
    }

    public void calculateMetrics(Collection<ProcessControlBlock> processes) {
        this.processes = processes;
        
        // Reset metrics
        throughput = 0;
        waitingTime = 0;
        turnAroundTime = 0;
        responseTime = 0;
        
        int numProcesses = processes.size();
        if (numProcesses == 0) {
            log("No processes to calculate metrics for");
            return;
        }

        for (ProcessControlBlock process : processes) {
            waitingTime += process.getWaitingTime();
            turnAroundTime += process.getTurnAroundTime();
            Integer respTime = process.getResponseTime();
            if (respTime != null) {
                responseTime += respTime;
            }
        }

        throughput = numProcesses / turnAroundTime;
        waitingTime /= numProcesses;
        turnAroundTime /= numProcesses;
        responseTime /= numProcesses;

        printMetrics();
        
        // Automatically write metrics to file
        String filename = schedulerType + "_q" + quantum;
        if (quantum2 > 0) {
            filename += "_q2_" + quantum2;
        }
        outputToFile(filename);
    }

    private void printMetrics() {
        log("--------- System Metrics ---------");
        log("Scheduler Type: " + schedulerType);
        if (quantum > 0) {
            log("Quantum: " + quantum);
        }
        if (quantum2 > 0) {
            log("Quantum2: " + quantum2);
        }
        log("Average Throughput: " + throughput);
        log("Average Waiting Time: " + waitingTime);
        log("Average Turnaround Time: " + turnAroundTime);
        log("Average Response Time: " + responseTime);
    }
    
    /**
     * Outputs metrics to a file
     * @param filename The name of the file to output to
     */
    public void outputToFile(String filename) {
        if (processes == null || processes.isEmpty()) {
            logError("No metrics to output to file");
            return;
        }
        
        String filePath = OUTPUT_DIRECTORY + File.separator + filename + ".txt";
        
        try (FileWriter writer = new FileWriter(filePath)) {
            // Write system metrics
            writer.write("--------- System Metrics ---------\n");
            writer.write("Scheduler Type: " + schedulerType + "\n");
            if (quantum > 0) {
                writer.write("Quantum: " + quantum + "\n");
            }
            if (quantum2 > 0) {
                writer.write("Quantum2: " + quantum2 + "\n");
            }
            writer.write("Average Throughput: " + throughput + "\n");
            writer.write("Average Waiting Time: " + waitingTime + "\n");
            writer.write("Average Turnaround Time: " + turnAroundTime + "\n");
            writer.write("Average Response Time: " + responseTime + "\n\n");
            
            // Write process-specific metrics
            writer.write("--------- Process Metrics ---------\n");
            writer.write("PID\tProcess Name\tWaiting Time\tTurnaround Time\tResponse Time\n");
            
            for (ProcessControlBlock process : processes) {
                writer.write(String.format("%d\t%s\t%d\t%d\t%d\n",
                    process.getPid(),
                    process.getFilePath(),
                    process.getWaitingTime(),
                    process.getTurnAroundTime(),
                    process.getResponseTime() != null ? process.getResponseTime() : 0));
            }
            
            log("Metrics output to file: " + filePath);
        } catch (IOException e) {
            logError("Failed to write metrics to file: " + e.getMessage());
        }
    }
}
