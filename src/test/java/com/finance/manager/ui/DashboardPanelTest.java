package com.finance.manager.ui;

import com.finance.manager.BudgetConfig;
import com.finance.manager.Expense;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DashboardPanelTest {

    private final BudgetConfig monthlyBudget = new BudgetConfig(5000, "monthly");
    private final LocalDate today = LocalDate.of(2026, 6, 19);

    @Test
    void historicalImportsUseLatestTransactionPeriod() {
        List<Expense> expenses = List.of(
                expense(LocalDate.of(2025, 1, 3)),
                expense(LocalDate.of(2025, 3, 28)));

        assertEquals(LocalDate.of(2025, 3, 28),
                DashboardPanel.resolveBudgetAnchor(expenses, monthlyBudget, today));
    }

    @Test
    void currentPeriodDataKeepsCurrentPeriodSelected() {
        List<Expense> expenses = List.of(
                expense(LocalDate.of(2025, 3, 28)),
                expense(LocalDate.of(2026, 6, 2)));

        assertEquals(today,
                DashboardPanel.resolveBudgetAnchor(expenses, monthlyBudget, today));
    }

    private static Expense expense(LocalDate date) {
        return new Expense(10, "Food", date, "test");
    }
}
