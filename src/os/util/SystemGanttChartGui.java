package os.util;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.GanttRenderer;
import org.jfree.data.gantt.Task;
import org.jfree.data.gantt.TaskSeries;
import org.jfree.data.gantt.TaskSeriesCollection;
import org.jfree.data.time.SimpleTimePeriod;
import os.ProcessControlBlock;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * THIS CLASS IS NOT WORKING LIKE WE WANT RIGHT NOW
 * SystemGanttChartGui is a utility class that generates a Gantt Chart for the system-wide process timeline.
 * This class uses JFreeChart to create a Gantt Chart with multiple queues and processes.
 */
public class SystemGanttChartGui {

    public static void makeChart(List<ProcessControlBlock> currentProcesses) {
        TaskSeriesCollection dataset = new TaskSeriesCollection();

        // Define Queue Labels on the Y-Axis
        String[] queues = {"Ready Queue", "Running Queue", "IO Queue", "MFQ 1", "MFQ 2", "MFQ 3"};
        Map<String, TaskSeries> queueSeriesMap = new HashMap<>();

        for (String queue : queues) {
            queueSeriesMap.put(queue, new TaskSeries(queue));
        }

        // Assign each process a unique color
        Map<Integer, Color> processColors = new HashMap<>();
        Color[] colors = {Color.RED, Color.BLUE, Color.GREEN, Color.ORANGE, Color.MAGENTA, Color.CYAN, Color.PINK};
        int colorIndex = 0;

        for (ProcessControlBlock pcb : currentProcesses) {
            processColors.put(pcb.getPid(), colors[colorIndex % colors.length]);
            colorIndex++;
        }

        int minTime = Integer.MAX_VALUE;
        int maxTime = Integer.MIN_VALUE;

        // Iterate over each process and create tasks per queue type
        for (ProcessControlBlock pcb : currentProcesses) {
            for (ProcessExecutionBurst burst : pcb.getTimeLine()) {
                int start = burst.getStart();
                int end = burst.getEnd();

                // Track min/max times
                minTime = Math.min(minTime, start);
                maxTime = Math.max(maxTime, end);

                String queueName;
                switch (burst.getQueueId()) {
                    case JOB_QUEUE:
                        queueName = "Ready Queue";
                        break;
                    case RUNNING_QUEUE:
                        queueName = "Running Queue";
                        break;
                    case IO_QUEUE:
                        queueName = "IO Queue";
                        break;
                    case MFQ_QUEUE_1:
                        queueName = "MFQ 1";
                        break;
                    case MFQ_QUEUE_2:
                        queueName = "MFQ 2";
                        break;
                    case MFQ_QUEUE_3:
                        queueName = "MFQ 3";
                        break;
                    default:
                        continue;
                }

                TaskSeries queueSeries = queueSeriesMap.get(queueName);
                if (queueSeries != null) {
                    Task burstTask = new Task("PID " + pcb.getPid(),
                            new SimpleTimePeriod(start, end));
                    queueSeries.add(burstTask);
                }
            }
        }

        // Add all queue series to dataset
        for (TaskSeries series : queueSeriesMap.values()) {
            dataset.add(series);
        }

        // Create the Gantt Chart
        JFreeChart chart = ChartFactory.createGanttChart(
                "System-Wide Gantt Chart",
                "Queues",
                "Clock Time",
                dataset,
                true,
                true,
                false
        );

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setDomainGridlinesVisible(true);
        plot.setRangeGridlinesVisible(true);

        // Set X-Axis (Clock Time)
        NumberAxis timeAxis = new NumberAxis("Clock Time (Ticks)");
        timeAxis.setAutoRange(false);
        timeAxis.setRange(minTime - 1, maxTime + 1);
        timeAxis.setTickUnit(new org.jfree.chart.axis.NumberTickUnit(1));
        plot.setRangeAxis(timeAxis);

        // Set Y-Axis (Queue Labels)
        CategoryAxis categoryAxis = plot.getDomainAxis();
        categoryAxis.setCategoryMargin(0.2);  // Add spacing between queues

        // ✅ Improved Renderer to Fix NumberFormatException
        GanttRenderer renderer = new GanttRenderer() {
            @Override
            public Paint getItemPaint(int row, int column) {
                try {
                    // Get Process ID from Task Name safely
                    String taskName = dataset.getRowKey(row).toString().replaceAll("[^0-9]", "").trim();

                    if (!taskName.isEmpty()) {
                        int pid = Integer.parseInt(taskName);
                        return processColors.getOrDefault(pid, Color.GRAY); // Use gray if no color found
                    }
                } catch (Exception e) {
                    System.err.println("Error parsing PID for Gantt Chart: " + e.getMessage());
                }
                return Color.GRAY; // Default to gray if parsing fails
            }
        };

        plot.setRenderer(renderer);

        // Display in GUI
        JFrame frame = new JFrame("System Gantt Chart");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(new ChartPanel(chart));
        frame.pack();
        frame.setVisible(true);

        // Save chart as PNG
        try {
            File file = new File("system_gantt_chart.png");
            org.jfree.chart.ChartUtils.saveChartAsPNG(file, chart, 1200, 700);
            System.out.println("✅ Gantt Chart saved as system_gantt_chart.png");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
