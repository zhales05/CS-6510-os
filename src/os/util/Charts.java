package os.util;

import org.jzy3d.chart.Chart;
import org.jzy3d.chart.factories.AWTChartFactory;
import org.jzy3d.chart.factories.IChartFactory;
import org.jzy3d.colors.Color;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.plot3d.primitives.Scatter;
import org.jzy3d.plot3d.rendering.canvas.Quality;

import java.util.List;
import java.util.Random;

public class Charts {
    private static List<MetricsTracker> metricsList;

    public static void setMetrics(List<MetricsTracker> metrics) {
        metricsList = metrics;
    }

    public static void start() {
        // Generate scatter data
        int size = metricsList.size();
        double quantum1;
        double quantum2;
        double metric;
        float a;

        Coord3d[] points = new Coord3d[size];
        Color[] colors = new Color[size];

        for (int i = 0; i < size; i++) {
            quantum1 = metricsList.get(i).getQuantum1() - 0.5;
            quantum2 = metricsList.get(i).getQuantum2() - 0.5;
            metric = metricsList.get(i).getResponseTime() - 0.5;
            points[i] = new Coord3d(quantum1, quantum2, metric);
            a = 0.25f;
            colors[i] = new Color(quantum1, quantum2, metric, a);
        }

        // Create a drawable scatter
        Scatter scatter = new Scatter(points, colors);
        scatter.setWidth(5); // Increase the size of the points


        // Open and show chart
        Quality q = Quality.Advanced();
        IChartFactory f = new AWTChartFactory();
        Chart chart = f.newChart(q);
        chart.getScene().add(scatter);
        chart.open();
        chart.addMouse();

        // Set custom axis labels
        chart.getAxisLayout().setXAxisLabel("Quantum 1");
        chart.getAxisLayout().setYAxisLabel("Quantum 2");
        chart.getAxisLayout().setZAxisLabel("Response Time");
    }

    public static void launchChart(List<MetricsTracker> metricsList) {
        Charts.setMetrics(metricsList);
        start();
    }
}

