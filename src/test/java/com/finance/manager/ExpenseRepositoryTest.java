package com.finance.manager;

import com.finance.manager.repository.ExpenseRepository;
import com.finance.manager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ExpenseRepositoryTest {
    @Autowired ExpenseRepository expenses;
    @Autowired UserRepository users;
    User alice;
    User bob;

    @BeforeEach
    void setUp() {
        expenses.deleteAll();
        users.deleteAll();
        alice = users.save(new User("alice", "hash"));
        bob = users.save(new User("bob", "hash"));
    }

    @Test
    void queriesAreIsolatedByOwner() {
        expenses.save(owned(alice, "10.10", "Food"));
        expenses.save(owned(bob, "99.99", "Rent"));

        assertThat(expenses.findAllByOwnerOrderByDateDescIdDesc(alice))
                .extracting(Expense::getAmount).containsExactly(new BigDecimal("10.10"));
        assertThat(expenses.sumAmountsByOwner(alice)).isEqualByComparingTo("10.10");
    }

    @Test
    void ownerAndCategoryQueryCannotLeakAnotherUsersRows() {
        expenses.save(owned(alice, "5.00", "Food"));
        expenses.save(owned(bob, "7.00", "Food"));

        assertThat(expenses.findByOwnerAndCategoryIgnoreCase(alice, "FOOD")).hasSize(1)
                .first().extracting(Expense::getOwner).isEqualTo(alice);
    }

    private static Expense owned(User owner, String amount, String category) {
        Expense expense = new Expense(new BigDecimal(amount), category, LocalDate.of(2026, 6, 1), "test");
        expense.assignTo(owner);
        return expense;
    }
}
