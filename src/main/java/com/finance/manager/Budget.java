package com.finance.manager;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** A single budget owned by one user and persisted through Hibernate. */
@Entity
@Table(name = "budget", uniqueConstraints =
        @UniqueConstraint(name = "uk_budget_user", columnNames = "user_id"))
public class Budget {

    @Id
    private Long id;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 16)
    private String period;

    /** Nullable only while the startup migration assigns the original v1 row. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User owner;

    protected Budget() {}

    public Budget(User owner, BigDecimal amount, String period) {
        this.owner = owner;
        this.id = owner == null ? 1L : owner.getId();
        update(new BudgetConfig(amount, period));
    }

    public Budget(Double amount, String period) {
        this(null, BigDecimal.valueOf(amount), period);
    }

    public Long getId() { return id; }
    public BigDecimal getAmount() { return amount; }
    public String getPeriod() { return period; }
    public User getOwner() { return owner; }

    public void assignTo(User owner) {
        this.owner = owner;
        if (id == null) id = owner.getId();
    }

    public void update(BudgetConfig config) {
        this.amount = config.amount().setScale(2, RoundingMode.HALF_EVEN);
        this.period = config.period();
    }

    public BudgetConfig toConfig() { return new BudgetConfig(amount, period); }
}
