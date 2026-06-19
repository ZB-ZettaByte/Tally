package com.finance.manager.api;

import com.finance.manager.BudgetConfig;

import java.math.BigDecimal;

public record BudgetResponse(BigDecimal amount, String period, boolean configured) {
    public static BudgetResponse from(BudgetConfig budget) {
        return new BudgetResponse(budget.amount(), budget.period(), budget.isSet());
    }
}
