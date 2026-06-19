package com.finance.manager;

import com.finance.manager.service.BudgetService;
import com.finance.manager.service.ExpenseService;
import com.finance.manager.ui.DashboardPanel;
import com.finance.manager.ui.ExpenseTablePanel;
import com.finance.manager.ui.TrendPanel;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import org.springframework.stereotype.Component;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Swing UI root — wired by Spring as a singleton {@link Component}.
 * {@link #show()} is called from {@link FinanceManagerApplication} on the EDT
 * after the Spring context finishes starting.
 */
@Component
public class MainApp {

    private final ExpenseService expenseService;
    private final BudgetService budgetService;

    private BudgetConfig budgetConfig;
    private User activeUser;
    private boolean darkMode = false;
    private JFrame frame;
    private DashboardPanel dashboardPanel;
    private TrendPanel trendPanel;
    private ExpenseTablePanel expensePanel;

    public MainApp(ExpenseService expenseService, BudgetService budgetService) {
        this.expenseService = expenseService;
        this.budgetService = budgetService;
    }

    /** Entry point called from the EDT after Spring Boot starts. */
    public void show(User user) {
        FlatLightLaf.setup();
        activeUser = user;
        budgetConfig = budgetService.load(activeUser);
        buildFrame();
    }

    // ── Frame assembly ────────────────────────────────────────────────────────

    private void buildFrame() {
        frame = new JFrame("Tally — Personal Finance Manager");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setSize(1120, 720);
        frame.setMinimumSize(new Dimension(860, 560));
        frame.setLayout(new BorderLayout());

        dashboardPanel = new DashboardPanel();
        trendPanel = new TrendPanel();
        // Array holder lets the lambda capture expTab before assignment completes
        ExpenseTablePanel[] expTabHolder = new ExpenseTablePanel[1];
        expTabHolder[0] = new ExpenseTablePanel(
                expenseService,
                activeUser,
                this::refreshAll);
        expensePanel = expTabHolder[0];

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBorder(BorderFactory.createEmptyBorder(0, 14, 12, 14));
        tabs.addTab("  Overview  ", dashboardPanel);
        tabs.addTab("  Expenses  ", expensePanel);
        tabs.addTab("  Trends  ", trendPanel);
        frame.add(tabs, BorderLayout.CENTER);
        frame.add(buildTopBar(), BorderLayout.NORTH);

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                onClose();
            }
        });

        refreshAll();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout(16, 0));
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        bar.setBorder(BorderFactory.createEmptyBorder(14, 20, 12, 20));

        JPanel branding = new JPanel();
        branding.setLayout(new BoxLayout(branding, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Tally");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        JLabel subtitle = new JLabel("Your money, clearly understood");
        subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 12f));
        subtitle.setForeground(UIManager.getColor("Label.disabledForeground"));
        branding.add(title);
        branding.add(subtitle);
        JLabel userLabel = new JLabel("Signed in as " + activeUser.getUsername());
        userLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        right.add(userLabel);

        JButton budgetBtn = new JButton("Set Budget");
        JButton darkBtn = new JButton("\u2600"); // ☀
        darkBtn.setFont(darkBtn.getFont().deriveFont(16f));
        darkBtn.setFocusPainted(false);
        darkBtn.setBorderPainted(false);
        darkBtn.setContentAreaFilled(false);
        darkBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        budgetBtn.addActionListener(e -> {
            showBudgetDialog();
        });
        darkBtn.addActionListener(e -> {
            darkMode = !darkMode;
            if (darkMode)
                FlatDarkLaf.setup();
            else
                FlatLightLaf.setup();
            SwingUtilities.updateComponentTreeUI(frame);
            dashboardPanel.applyTheme();
            trendPanel.applyTheme();
            frame.revalidate();
            frame.repaint();
            darkBtn.setText(darkMode ? "\ud83c\udf19" : "\u2600"); // 🌙 / ☀
        });

        right.add(budgetBtn);
        right.add(darkBtn);
        bar.add(branding, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    private void refreshAll() {
        List<Expense> expenses = expenseService.getAllExpenses(activeUser);
        dashboardPanel.refresh(expenses, budgetConfig);
        trendPanel.refresh(expenses);
        expensePanel.refresh(expenses);
    }

    // ── Budget dialog ─────────────────────────────────────────────────────────

    private void showBudgetDialog() {
        JTextField amountField = new JTextField(10);
        JComboBox<String> combo = new JComboBox<>(new String[] { "monthly", "weekly" });

        if (budgetConfig.isSet()) {
            amountField.setText(String.format("%.2f", budgetConfig.amount()));
            combo.setSelectedItem(budgetConfig.period());
        }

        JPanel panel = new JPanel(new GridLayout(2, 2, 6, 6));
        panel.add(new JLabel("Budget Amount ($):"));
        panel.add(amountField);
        panel.add(new JLabel("Period:"));
        panel.add(combo);

        int opt = JOptionPane.showConfirmDialog(frame, panel, "Set Budget",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opt != JOptionPane.OK_OPTION)
            return;

        try {
            java.math.BigDecimal amount = new java.math.BigDecimal(amountField.getText().trim());
            if (amount.signum() <= 0) {
                warn("Budget must be greater than zero.");
                return;
            }
            budgetConfig = new BudgetConfig(amount, (String) combo.getSelectedItem());
            budgetService.save(activeUser, budgetConfig);
            refreshAll();
            JOptionPane.showMessageDialog(frame,
                    String.format("Your %s budget is now $%,.2f.", budgetConfig.period(), amount),
                    "Budget Updated", JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException ex) {
            warn("Enter a valid amount, such as 1500.00.");
        } catch (RuntimeException ex) {
            warn("The budget could not be saved: " + ex.getMessage());
        }
    }

    // ── Window close ──────────────────────────────────────────────────────────

    private void onClose() {
        int choice = JOptionPane.showConfirmDialog(frame,
                "Export expenses before closing?", "Export",
                JOptionPane.YES_NO_CANCEL_OPTION);
        if (choice == JOptionPane.CANCEL_OPTION)
            return;
        if (choice == JOptionPane.YES_OPTION) {
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new java.io.File("expenses.csv"));
            if (fc.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                String path = fc.getSelectedFile().getAbsolutePath();
                if (!path.endsWith(".csv"))
                    path += ".csv";
                expenseService.exportToCSV(activeUser, path);
            }
        }
        System.exit(0);
    }

    private void warn(String msg) {
        JOptionPane.showMessageDialog(frame, msg, "Validation Error", JOptionPane.WARNING_MESSAGE);
    }
}
