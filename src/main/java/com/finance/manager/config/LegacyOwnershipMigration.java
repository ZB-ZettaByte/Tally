package com.finance.manager.config;

import com.finance.manager.Budget;
import com.finance.manager.Expense;
import com.finance.manager.User;
import com.finance.manager.repository.BudgetRepository;
import com.finance.manager.repository.ExpenseRepository;
import com.finance.manager.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Assigns v1 global rows to the oldest account once, preserving existing local data. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LegacyOwnershipMigration implements ApplicationRunner {
    private final UserRepository users;
    private final ExpenseRepository expenses;
    private final BudgetRepository budgets;

    public LegacyOwnershipMigration(UserRepository users, ExpenseRepository expenses,
                                    BudgetRepository budgets) {
        this.users = users;
        this.expenses = expenses;
        this.budgets = budgets;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        User owner = users.findFirstByOrderByIdAsc().orElse(null);
        if (owner == null) return;

        List<Expense> legacyExpenses = expenses.findAllByOwnerIsNull();
        legacyExpenses.forEach(expense -> expense.assignTo(owner));
        expenses.saveAll(legacyExpenses);

        List<Budget> legacyBudgets = budgets.findAllByOwnerIsNull();
        legacyBudgets.forEach(budget -> budget.assignTo(owner));
        budgets.saveAll(legacyBudgets);
    }
}
