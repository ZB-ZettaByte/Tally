package com.finance.manager.config;

import com.finance.manager.BudgetConfig;
import com.finance.manager.Expense;
import com.finance.manager.User;
import com.finance.manager.service.BudgetService;
import com.finance.manager.service.ExpenseService;
import com.finance.manager.service.UserService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Deterministic demo account enabled only with the {@code demo} profile. */
@Component
@Profile("demo")
@Order(10)
public class DemoDataSeeder implements ApplicationRunner {
    private final UserService users;
    private final ExpenseService expenses;
    private final BudgetService budgets;

    public DemoDataSeeder(UserService users, ExpenseService expenses, BudgetService budgets) {
        this.users = users;
        this.expenses = expenses;
        this.budgets = budgets;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        User demo;
        try {
            demo = users.requireByUsername("tally-demo");
        } catch (IllegalArgumentException missing) {
            demo = users.register("tally-demo", "demo123");
        }
        if (!expenses.getAllExpenses(demo).isEmpty()) return;

        LocalDate month = LocalDate.now().withDayOfMonth(1);
        add(demo, "42.80", "Food", month.plusDays(1), "Weekly groceries");
        add(demo, "1250.00", "Rent", month.plusDays(2), "Monthly rent");
        add(demo, "68.40", "Transport", month.plusDays(4), "Train pass");
        add(demo, "94.20", "Utilities", month.plusDays(6), "Electricity");
        add(demo, "18.75", "Food", month.plusDays(8), "Lunch");
        add(demo, "54.00", "Health", month.plusDays(10), "Pharmacy");
        add(demo, "31.99", "Entertainment", month.plusDays(12), "Streaming and cinema");
        add(demo, "76.15", "Shopping", month.plusDays(14), "Household supplies");
        budgets.save(demo, new BudgetConfig(new BigDecimal("2500.00"), "monthly"));
    }

    private void add(User owner, String amount, String category, LocalDate date, String description) {
        expenses.addExpense(owner, new Expense(new BigDecimal(amount), category, date, description));
    }
}
