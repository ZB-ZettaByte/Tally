package com.finance.manager.service;

import com.finance.manager.CSVHandler;
import com.finance.manager.Expense;
import com.finance.manager.User;
import com.finance.manager.repository.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

/** Transactional use cases for expenses; every operation requires an owner. */
@Service
@Transactional
public class ExpenseService {
    private final ExpenseRepository expenseRepository;
    private final CSVHandler csvHandler;

    public ExpenseService(ExpenseRepository expenseRepository, CSVHandler csvHandler) {
        this.expenseRepository = expenseRepository;
        this.csvHandler = csvHandler;
    }

    public Expense addExpense(User owner, Expense expense) {
        expense.assignTo(owner);
        return expenseRepository.save(expense);
    }

    public void deleteExpense(User owner, Long id) {
        Expense expense = expenseRepository.findByIdAndOwner(id, owner)
                .orElseThrow(() -> new NoSuchElementException("Expense not found."));
        expenseRepository.delete(expense);
    }

    public Expense updateExpense(User owner, Long id, Expense replacement) {
        Expense expense = getExpense(owner, id);
        expense.update(replacement.getAmount(), replacement.getCategory(),
                replacement.getDate(), replacement.getDescription());
        return expense;
    }

    public void clearAll(User owner) { expenseRepository.deleteAllByOwner(owner); }

    public void importFromCSV(User owner, String filePath) {
        List<Expense> loaded = csvHandler.loadExpensesFromCSV(filePath);
        loaded.forEach(expense -> expense.assignTo(owner));
        expenseRepository.deleteAllByOwner(owner);
        expenseRepository.saveAll(loaded);
    }

    @Transactional(readOnly = true)
    public List<Expense> getAllExpenses(User owner) {
        return expenseRepository.findAllByOwnerOrderByDateDescIdDesc(owner);
    }

    @Transactional(readOnly = true)
    public Expense getExpense(User owner, Long id) {
        return expenseRepository.findByIdAndOwner(id, owner)
                .orElseThrow(() -> new NoSuchElementException("Expense not found."));
    }

    @Transactional(readOnly = true)
    public void exportToCSV(User owner, String filePath) {
        csvHandler.exportExpensesToCSV(filePath, getAllExpenses(owner));
    }
}
