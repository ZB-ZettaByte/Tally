package com.finance.manager.repository;

import com.finance.manager.Budget;
import com.finance.manager.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** One budget per user, enforced by a database unique constraint. */
public interface BudgetRepository extends JpaRepository<Budget, Long> {
    Optional<Budget> findByOwner(User owner);
    List<Budget> findAllByOwnerIsNull();
}
