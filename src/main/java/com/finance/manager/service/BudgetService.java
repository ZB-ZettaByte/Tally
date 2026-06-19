package com.finance.manager.service;

import com.finance.manager.Budget;
import com.finance.manager.BudgetConfig;
import com.finance.manager.User;
import com.finance.manager.repository.BudgetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Transactional, user-scoped budget use cases. */
@Service
@Transactional
public class BudgetService {
    private final BudgetRepository budgetRepository;

    public BudgetService(BudgetRepository budgetRepository) {
        this.budgetRepository = budgetRepository;
    }

    public BudgetConfig save(User owner, BudgetConfig config) {
        Budget budget = budgetRepository.findByOwner(owner)
                .orElseGet(() -> new Budget(owner, config.amount(), config.period()));
        budget.update(config);
        return budgetRepository.save(budget).toConfig();
    }

    @Transactional(readOnly = true)
    public BudgetConfig load(User owner) {
        return budgetRepository.findByOwner(owner).map(Budget::toConfig).orElse(BudgetConfig.UNSET);
    }
}
