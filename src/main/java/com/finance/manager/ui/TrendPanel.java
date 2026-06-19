package com.finance.manager.ui;

import com.finance.manager.Expense;
import com.finance.manager.analytics.SpendingAnalytics;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Monthly spending trend with an easy-to-compare line and rolling average. */
public class TrendPanel extends JPanel {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MMM yy");
    private static final Color ACCENT = new Color(72, 101, 255);
    private static final Color SECONDARY = new Color(241, 157, 56);

    private final SpendingAnalytics analytics = new SpendingAnalytics();
    private final DefaultCategoryDataset spendingDataset = new DefaultCategoryDataset();
    private final DefaultCategoryDataset averageDataset = new DefaultCategoryDataset();
    private final JLabel forecastLabel = statValue();
    private final JLabel changeLabel = statValue();
    private final JLabel averageLabel = statValue();
    private final JLabel insightLabel = new JLabel("Add expenses across multiple months to see a trend.");
    private final JFreeChart chart;

    public TrendPanel() {
        setLayout(new BorderLayout(12, 12));
        setBorder(new EmptyBorder(14, 4, 4, 4));

        JPanel heading = new JPanel(new BorderLayout());
        JPanel copy = new JPanel();
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Spending over time");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        JLabel subtitle = new JLabel("Monthly totals with a rolling three-month average");
        subtitle.setForeground(UIManager.getColor("Label.disabledForeground"));
        copy.add(title);
        copy.add(subtitle);
        heading.add(copy, BorderLayout.WEST);
        add(heading, BorderLayout.NORTH);

        chart = ChartFactory.createBarChart(
                null, "Month", "Amount ($)", spendingDataset,
                PlotOrientation.VERTICAL, true, true, false);
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setOutlineVisible(false);
        plot.setRangeGridlinePaint(new Color(220, 224, 234));
        plot.setDomainGridlinesVisible(false);

        plot.getRangeAxis().setUpperMargin(0.18);

        BarRenderer bars = (BarRenderer) plot.getRenderer();
        bars.setSeriesPaint(0, ACCENT);
        bars.setBarPainter(new StandardBarPainter());
        bars.setDrawBarOutline(false);
        bars.setShadowVisible(false);
        bars.setMaximumBarWidth(0.12);
        bars.setDefaultItemLabelGenerator(new StandardCategoryItemLabelGenerator(
                "{2}", java.text.NumberFormat.getCurrencyInstance()));
        bars.setDefaultItemLabelsVisible(true);

        plot.setDataset(1, averageDataset);
        LineAndShapeRenderer averageLine = new LineAndShapeRenderer(true, true);
        averageLine.setSeriesPaint(0, SECONDARY);
        averageLine.setSeriesStroke(0, new BasicStroke(3f, BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND, 1f, new float[]{7f, 5f}, 0f));
        plot.setRenderer(1, averageLine);

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setMouseWheelEnabled(false);
        chartPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 224, 234)),
                new EmptyBorder(8, 8, 8, 8)));
        add(chartPanel, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(12, 10));
        JPanel stats = new JPanel(new GridLayout(1, 3, 12, 0));
        stats.add(statCard("NEXT MONTH FORECAST", forecastLabel));
        stats.add(statCard("LATEST CHANGE", changeLabel));
        stats.add(statCard("MONTHLY AVERAGE", averageLabel));
        footer.add(stats, BorderLayout.CENTER);
        insightLabel.setFont(insightLabel.getFont().deriveFont(Font.ITALIC, 12f));
        insightLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        insightLabel.setBorder(new EmptyBorder(2, 2, 0, 2));
        footer.add(insightLabel, BorderLayout.SOUTH);
        add(footer, BorderLayout.SOUTH);
        applyTheme();
    }

    public void refresh(List<Expense> expenses) {
        spendingDataset.clear();
        averageDataset.clear();
        Map<YearMonth, Double> monthly = analytics.monthlyTotals(expenses);
        List<Double> values = new ArrayList<>(monthly.values());
        List<YearMonth> months = new ArrayList<>(monthly.keySet());

        for (int i = 0; i < months.size(); i++) {
            String label = months.get(i).format(MONTH_FMT);
            spendingDataset.addValue(values.get(i), "Monthly spend", label);
            int start = Math.max(0, i - 2);
            double movingAverage = values.subList(start, i + 1).stream()
                    .mapToDouble(Double::doubleValue).average().orElse(0);
            averageDataset.addValue(movingAverage, "Rolling average", label);
        }

        double forecast = analytics.forecastNextMonth(expenses);
        forecastLabel.setText(monthly.size() < 2 ? "—" : money(forecast));
        averageLabel.setText(values.isEmpty() ? "—"
                : money(values.stream().mapToDouble(Double::doubleValue).average().orElse(0)));

        if (values.size() >= 2) {
            double last = values.get(values.size() - 1);
            double previous = values.get(values.size() - 2);
            double delta = last - previous;
            double percent = previous == 0 ? 0 : Math.abs(delta / previous) * 100;
            changeLabel.setText((delta >= 0 ? "+" : "−") + String.format("%.1f%%", percent));
            changeLabel.setForeground(delta > 0 ? new Color(220, 74, 84) : new Color(28, 157, 104));
            insightLabel.setText(delta > 0
                    ? "Spending increased in the latest recorded month. Check the category view for the driver."
                    : "Spending decreased in the latest recorded month — nice work keeping it down.");
        } else {
            changeLabel.setText("—");
            insightLabel.setText("Add expenses across multiple months to see a trend.");
        }
        revalidate();
        repaint();
    }

    public void applyTheme() {
        Color background = UIManager.getColor("Panel.background");
        chart.setBackgroundPaint(background);
        chart.getCategoryPlot().setBackgroundPaint(background);
    }

    private JPanel statCard(String title, JLabel value) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 224, 234)),
                new EmptyBorder(10, 13, 10, 13)));
        JLabel label = new JLabel(title);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 10f));
        label.setForeground(UIManager.getColor("Label.disabledForeground"));
        card.add(label, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    private static JLabel statValue() {
        JLabel label = new JLabel("—");
        label.setFont(label.getFont().deriveFont(Font.BOLD, 18f));
        return label;
    }

    private static String money(double amount) {
        return String.format("$%,.2f", amount);
    }
}
