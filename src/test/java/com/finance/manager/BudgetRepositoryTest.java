package com.finance.manager;

import com.finance.manager.repository.BudgetRepository;
import com.finance.manager.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class BudgetRepositoryTest {
    @Autowired BudgetRepository budgets;
    @Autowired UserRepository users;

    @Test
    void eachUserHasAnIndependentBudget() {
        User alice = users.save(new User("alice", "hash"));
        User bob = users.save(new User("bob", "hash"));
        budgets.save(new Budget(alice, new BigDecimal("1500.00"), "monthly"));
        budgets.save(new Budget(bob, new BigDecimal("250.00"), "weekly"));

        assertThat(budgets.findByOwner(alice).orElseThrow().getAmount()).isEqualByComparingTo("1500.00");
        assertThat(budgets.findByOwner(bob).orElseThrow().getAmount()).isEqualByComparingTo("250.00");
        assertThat(budgets.count()).isEqualTo(2);
    }
}
