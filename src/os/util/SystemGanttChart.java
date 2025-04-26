package os.util;

import os.ProcessControlBlock;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class SystemGanttChart implements Logging {
    public static void makeChart(List<ProcessControlBlock> currentProcesses) {
        StringBuilder sb = new StringBuilder("System-Wide Gantt Chart:\n");

        // Use a TreeMap to maintain execution order based on time
        TreeMap<Integer, String[]> systemTimeline = new TreeMap<>();

        // Define column width for alignment
        final int COLUMN_WIDTH = 8; // Adjust this for better spacing

        // Initialize timeline with empty states
        for (ProcessControlBlock pcb : currentProcesses) {
            for (ProcessExecutionBurst burst : pcb.getTimeLine()) {
                for (int t = burst.getStart(); t < burst.getEnd(); t++) {
                    if (!systemTimeline.containsKey(t)) {
                        systemTimeline.put(t, new String[]{" ", " ", " ", " ", " ", " ", " "});
                    }
                }
            }
        }

        // Populate the system-wide timeline with **multiple PIDs** per queue
        for (ProcessControlBlock pcb : currentProcesses) {
            String pidLabel = String.format("%2d", pcb.getPid()); // Ensure consistent spacing
            for (ProcessExecutionBurst burst : pcb.getTimeLine()) {
                for (int t = burst.getStart(); t < burst.getEnd(); t++) {
                    String[] queues = systemTimeline.get(t);
                    switch (burst.getQueueId()) {
                        case JOB_QUEUE:
                            queues[0] = appendPid(queues[0], pidLabel);
                            break;
                        case RUNNING_QUEUE:
                            queues[1] = appendPid(queues[1], pidLabel);
                            break;
                        case IO_QUEUE:
                            queues[2] = appendPid(queues[2], pidLabel);
                            break;
                        case RR_QUEUE, FCFS_QUEUE:
                            queues[3] = appendPid(queues[3], pidLabel);
                            break;
                        case MFQ_QUEUE_1:
                            queues[4] = appendPid(queues[4], pidLabel);
                            break;
                        case MFQ_QUEUE_2:
                            queues[5] = appendPid(queues[5], pidLabel);
                            break;
                        case MFQ_QUEUE_3:
                            queues[6] = appendPid(queues[6], pidLabel);
                            break;
                    }
                }
            }
        }

        // Print header with separators
        sb.append("Time:    |");
        for (int time : systemTimeline.keySet()) {
            sb.append(String.format("%" + COLUMN_WIDTH + "d|", time));
        }
        sb.append("\n");

        // Print queue states with **comma-separated PIDs** and separators
        String[] queueNames = {"Job:     |", "Running: |", "IO:      |", "Ready:   |", "MFQ 1:   |", "MFQ 2:   |", "MFQ 3:   |"};
        for (int i = 0; i < queueNames.length; i++) {
            sb.append(queueNames[i]);
            for (int time : systemTimeline.keySet()) {
                sb.append(String.format("%" + COLUMN_WIDTH + "s|", systemTimeline.get(time)[i]));
            }
            sb.append("\n");
        }

        // ✅ Ensure the file exists before writing
        File file = new File("system_gantt_chart.txt");
        try {
            if (file.createNewFile()) {
                System.out.println("✅ Created new file: system_gantt_chart.txt");
            } else {
                System.out.println("📄 File already exists. Overwriting...");
            }
        } catch (IOException e) {
            System.err.println("❌ Error creating file: " + e.getMessage());
            return; // Exit method if file creation fails
        }

        // ✅ Write to file
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(sb.toString());
            System.out.println("✅ System Gantt Chart saved to system_gantt_chart.txt");
        } catch (IOException e) {
            System.err.println("❌ Error writing System Gantt Chart to file: " + e.getMessage());
        }
    }

    /**
     * Appends a process PID to an existing queue entry.
     * Ensures multiple processes at the same time are shown properly.
     */
    private static String appendPid(String existing, String newPid) {
        if (existing.trim().isEmpty()) {
            return newPid;
        }
        return existing + "," + newPid;  // Comma-separated list of PIDs
    }
}
