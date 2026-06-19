package com.finance.manager.api;

import com.finance.manager.Expense;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseResponse(Long id, BigDecimal amount, String category,
                              LocalDate date, String description) {
    public static ExpenseResponse from(Expense expense) {
        return new ExpenseResponse(expense.getId(), expense.getAmount(), expense.getCategory(),
                expense.getDate(), expense.getDescription());
    }
}
