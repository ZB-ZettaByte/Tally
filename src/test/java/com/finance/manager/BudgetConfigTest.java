package com.finance.manager;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class BudgetConfigTest {

    private static final LocalDate WEDNESDAY = LocalDate.of(2026, 6, 17);

    @Test
    void monthlyBudgetIncludesOnlyCurrentCalendarMonth() {
        BudgetConfig budget = new BudgetConfig(1000, "monthly");

        assertTrue(budget.includes(LocalDate.of(2026, 6, 1), WEDNESDAY));
        assertTrue(budget.includes(LocalDate.of(2026, 6, 30), WEDNESDAY));
        assertFalse(budget.includes(LocalDate.of(2026, 5, 31), WEDNESDAY));
    }

    @Test
    void weeklyBudgetUsesMondayThroughSunday() {
        BudgetConfig budget = new BudgetConfig(250, "weekly");

        assertTrue(budget.includes(LocalDate.of(2026, 6, 15), WEDNESDAY));
        assertTrue(budget.includes(LocalDate.of(2026, 6, 21), WEDNESDAY));
        assertFalse(budget.includes(LocalDate.of(2026, 6, 14), WEDNESDAY));
        assertFalse(budget.includes(LocalDate.of(2026, 6, 22), WEDNESDAY));
    }
}
