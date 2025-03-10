package os.util;

import java.util.HashMap;
import java.util.Map;

public class MetricsTracker {
    private final Map<Integer, Double> throughput = new HashMap<>();

    int startTime;
    int endTime;

    //TODO: fix issues where -1 is the only id for FIFO causing it to just update not add another one
    private void addThroughput(int quantum, double throughput) {
        this.throughput.put(quantum, throughput);
    }

    public void addThroughput(int quantum, int completedProcesses, int startTime, int endTime) {
        addThroughput(quantum, calculateThroughput(completedProcesses, startTime, endTime));
    }

    public double calculateThroughput(int completedProcesses, int startTime, int endTime) {
        if (endTime - startTime == 0) return 0; // Prevent division by zero
        return (double) completedProcesses / (endTime - startTime);
    }

    public double getThroughput(int quantum) {
        return throughput.get(quantum);
    }

    public Map<Integer, Double> getThroughput() {
        return throughput;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("------ Metrics ------ \n");
        for (Map.Entry<Integer, Double> entry : throughput.entrySet()) {
            if (entry.getKey() == -1) {
                sb.append("quantum=FIFO, throughput=").append(entry.getValue()).append("\n");
            } else {
                sb.append("quantum=").append(entry.getKey())
                        .append(", throughput=").append(entry.getValue()).append("\n");
            }
        }
        return sb.toString();
    }
}
