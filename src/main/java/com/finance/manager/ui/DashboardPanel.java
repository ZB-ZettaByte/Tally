package com.finance.manager.ui;

import com.finance.manager.BudgetConfig;
import com.finance.manager.Expense;
import com.finance.manager.analytics.SpendingAnalytics;
import com.finance.manager.analytics.SpendingAnomaly;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Overview dashboard with readable category comparisons and budget status. */
public class DashboardPanel extends JPanel {

    private static final Color ACCENT = new Color(72, 101, 255);
    private static final Color SUCCESS = new Color(28, 157, 104);
    private static final Color WARNING = new Color(231, 154, 54);
    private static final Color DANGER = new Color(220, 74, 84);

    private final SpendingAnalytics analytics = new SpendingAnalytics();
    private final SpendingAnomaly anomalyDetector = new SpendingAnomaly();
    private final DefaultCategoryDataset categoryDataset = new DefaultCategoryDataset();
    private final JFreeChart categoryChart;

    private final JLabel totalLabel = valueLabel();
    private final JLabel totalTitleLabel = metricTitle("PERIOD SPEND");
    private final JLabel budgetLabel = valueLabel();
    private final JLabel remainLabel = valueLabel();
    private final JLabel topCatLabel = valueLabel();
    private final JLabel avgDayLabel = valueLabel();
    private final JLabel forecastLabel = valueLabel();
    private final JLabel anomalyLabel = valueLabel();
    private final JProgressBar budgetBar = new JProgressBar(0, 100);
    private final JLabel budgetStatus = new JLabel("Set a budget to track progress");

    public DashboardPanel() {
        setLayout(new BorderLayout(14, 14));
        setBorder(new EmptyBorder(14, 4, 4, 4));

        JPanel metrics = new JPanel(new GridLayout(1, 4, 12, 0));
        metrics.add(metricCard(totalTitleLabel, totalLabel));
        metrics.add(metricCard(metricTitle("BUDGET"), budgetLabel));
        metrics.add(metricCard(metricTitle("REMAINING"), remainLabel));
        metrics.add(metricCard(metricTitle("TOP CATEGORY"), topCatLabel));
        add(metrics, BorderLayout.NORTH);

        categoryChart = ChartFactory.createBarChart(
                "Spending by category", "Amount ($)", "",
                categoryDataset, PlotOrientation.HORIZONTAL, false, true, false);
        CategoryPlot plot = categoryChart.getCategoryPlot();
        plot.setOutlineVisible(false);
        plot.setRangeGridlinePaint(new Color(220, 224, 234));
        plot.setDomainGridlinesVisible(false);
        ((NumberAxis) plot.getRangeAxis()).setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, ACCENT);
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setDrawBarOutline(false);
        renderer.setShadowVisible(false);
        renderer.setMaximumBarWidth(0.09);
        renderer.setDefaultItemLabelGenerator(new StandardCategoryItemLabelGenerator("{2}",
                java.text.NumberFormat.getCurrencyInstance()));
        renderer.setDefaultItemLabelsVisible(true);

        ChartPanel chartPanel = new ChartPanel(categoryChart);
        chartPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 224, 234)),
                new EmptyBorder(6, 6, 6, 6)));
        chartPanel.setMouseWheelEnabled(false);
        add(chartPanel, BorderLayout.CENTER);

        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 224, 234)),
                new EmptyBorder(18, 18, 18, 18)));
        side.setPreferredSize(new Dimension(285, 360));

        JLabel heading = new JLabel("Budget health");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 17f));
        heading.setAlignmentX(LEFT_ALIGNMENT);
        side.add(heading);
        side.add(Box.createVerticalStrut(14));

        budgetBar.setStringPainted(true);
        budgetBar.setString("No budget set");
        budgetBar.setPreferredSize(new Dimension(240, 22));
        budgetBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        budgetBar.setAlignmentX(LEFT_ALIGNMENT);
        side.add(budgetBar);
        side.add(Box.createVerticalStrut(7));
        budgetStatus.setForeground(UIManager.getColor("Label.disabledForeground"));
        budgetStatus.setAlignmentX(LEFT_ALIGNMENT);
        side.add(budgetStatus);
        side.add(Box.createVerticalStrut(24));
        side.add(detailRow("Average per day", avgDayLabel));
        side.add(detailRow("Next month forecast", forecastLabel));
        side.add(detailRow("Unusual expenses", anomalyLabel));
        side.add(Box.createVerticalGlue());
        add(side, BorderLayout.EAST);

        applyTheme();
    }

    /** Syncs the overview with the current expenses and active budget period. */
    public void refresh(List<Expense> expenses, BudgetConfig budget) {
        LocalDate anchor = resolveBudgetAnchor(expenses, budget, LocalDate.now());
        List<Expense> periodExpenses = budget.isSet()
                ? expenses.stream().filter(e -> budget.includes(e.getDate(), anchor)).toList()
                : expenses;

        categoryDataset.clear();
        Map<String, Double> byCategory = analytics.spendingByCategory(periodExpenses);
        byCategory.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .forEach(entry -> categoryDataset.addValue(entry.getValue(), "Spending", entry.getKey()));

        double periodTotal = analytics.totalSpent(periodExpenses);
        double remain = budget.amountAsDouble() - periodTotal;
        Optional<String> topCat = analytics.topCategory(periodExpenses);
        double avgDay = analytics.averageDailySpend(expenses);
        double forecast = analytics.forecastNextMonth(expenses);
        int anomalies = anomalyDetector.detectAnomalies(expenses).size();

        totalTitleLabel.setText(budget.isSet() ? periodTitle(budget, anchor) : "TOTAL SPEND");
        totalLabel.setText(money(periodTotal));
        budgetLabel.setText(budget.isSet() ? money(budget.amountAsDouble()) : "Not set");
        remainLabel.setText(budget.isSet() ? money(remain) : "—");
        remainLabel.setForeground(budget.isSet() && remain < 0 ? DANGER : SUCCESS);
        topCatLabel.setText(topCat.orElse("—"));
        avgDayLabel.setText(money(avgDay));
        forecastLabel.setText(money(forecast));
        anomalyLabel.setText(anomalies == 0 ? "None" : anomalies + " flagged");
        anomalyLabel.setForeground(anomalies > 0 ? DANGER : SUCCESS);

        if (budget.isSet()) {
            int actualPct = (int) Math.round((periodTotal / budget.amountAsDouble()) * 100);
            budgetBar.setValue(Math.min(100, actualPct));
            budgetBar.setString(actualPct + "% used");
            budgetBar.setForeground(actualPct >= 100 ? DANGER : actualPct >= 75 ? WARNING : SUCCESS);
            budgetStatus.setText(periodDescription(budget, anchor) + " • " + money(Math.abs(remain))
                    + (remain >= 0 ? " available" : " over budget"));
        } else {
            budgetBar.setValue(0);
            budgetBar.setString("No budget set");
            budgetBar.setForeground(ACCENT);
            budgetStatus.setText("Set a budget to track progress");
        }
        revalidate();
        repaint();
    }

    public void applyTheme() {
        Color background = UIManager.getColor("Panel.background");
        categoryChart.setBackgroundPaint(background);
        categoryChart.getCategoryPlot().setBackgroundPaint(background);
        categoryChart.getTitle().setPaint(UIManager.getColor("Label.foreground"));
    }

    private JPanel metricCard(JLabel label, JLabel value) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 224, 234)),
                new EmptyBorder(13, 15, 13, 15)));
        card.add(label, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    private JPanel detailRow(String title, JLabel value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        row.add(new JLabel(title), BorderLayout.WEST);
        row.add(value, BorderLayout.EAST);
        return row;
    }

    private static JLabel valueLabel() {
        JLabel label = new JLabel("—");
        label.setFont(label.getFont().deriveFont(Font.BOLD, 20f));
        return label;
    }

    private static JLabel metricTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 10f));
        label.setForeground(UIManager.getColor("Label.disabledForeground"));
        return label;
    }

    static LocalDate resolveBudgetAnchor(List<Expense> expenses, BudgetConfig budget, LocalDate today) {
        if (!budget.isSet() || expenses.stream().anyMatch(e -> budget.includes(e.getDate(), today))) {
            return today;
        }
        return expenses.stream().map(Expense::getDate).max(LocalDate::compareTo).orElse(today);
    }

    private static String periodTitle(BudgetConfig budget, LocalDate anchor) {
        if ("weekly".equalsIgnoreCase(budget.period())) {
            return "WEEK OF " + anchor.format(DateTimeFormatter.ofPattern("MMM d")).toUpperCase() + " SPEND";
        }
        return anchor.format(DateTimeFormatter.ofPattern("MMM yyyy")).toUpperCase() + " SPEND";
    }

    private static String periodDescription(BudgetConfig budget, LocalDate anchor) {
        String date = "weekly".equalsIgnoreCase(budget.period())
                ? "Week of " + anchor.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
                : anchor.format(DateTimeFormatter.ofPattern("MMMM yyyy"));
        boolean historical = !budget.includes(LocalDate.now(), anchor);
        return date + (historical ? " (latest data)" : "");
    }

    private static String money(double amount) {
        return String.format("$%,.2f", amount);
    }
}
