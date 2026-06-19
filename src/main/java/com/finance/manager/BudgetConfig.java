package com.finance.manager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/** Immutable, decimal-safe budget value used outside the persistence layer. */
public record BudgetConfig(BigDecimal amount, String period) {

    public static final BudgetConfig UNSET = new BudgetConfig(BigDecimal.ZERO, "");

    public BudgetConfig {
        if (amount == null || amount.signum() < 0) {
            throw new IllegalArgumentException("Budget amount cannot be negative.");
        }
        amount = amount.setScale(2, RoundingMode.HALF_EVEN);
        period = period == null ? "" : period.trim().toLowerCase();
        if (!period.isBlank() && !period.equals("weekly") && !period.equals("monthly")) {
            throw new IllegalArgumentException("Budget period must be weekly or monthly.");
        }
    }

    public BudgetConfig(double amount, String period) {
        this(BigDecimal.valueOf(amount), period);
    }

    public boolean isSet() { return amount.signum() > 0 && !period.isBlank(); }
    public String displayPeriod() { return period.isBlank() ? "Not set" : period; }
    public double amountAsDouble() { return amount.doubleValue(); }

    public boolean includes(LocalDate date, LocalDate anchor) {
        if (date == null || anchor == null || !isSet()) return false;
        if ("weekly".equals(period)) {
            LocalDate start = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            return !date.isBefore(start) && !date.isAfter(start.plusDays(6));
        }
        return date.getYear() == anchor.getYear() && date.getMonth() == anchor.getMonth();
    }
}
