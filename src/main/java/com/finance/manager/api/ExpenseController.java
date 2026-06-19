package com.finance.manager.api;

import com.finance.manager.Expense;
import com.finance.manager.User;
import com.finance.manager.service.ExpenseService;
import com.finance.manager.service.OAuthIdentityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/expenses")
@Tag(name = "Expenses", description = "Authenticated user's expense ledger")
public class ExpenseController {
    private final ExpenseService expenses;
    private final OAuthIdentityService identities;

    public ExpenseController(ExpenseService expenses, OAuthIdentityService identities) {
        this.expenses = expenses;
        this.identities = identities;
    }

    @GetMapping
    @Operation(summary = "List the authenticated user's expenses")
    public List<ExpenseResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return expenses.getAllExpenses(owner(jwt)).stream().map(ExpenseResponse::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one owned expense")
    public ExpenseResponse get(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return ExpenseResponse.from(expenses.getExpense(owner(jwt), id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an expense using decimal-safe money")
    public ExpenseResponse create(@Valid @RequestBody ExpenseRequest request,
                                  @AuthenticationPrincipal Jwt jwt) {
        return ExpenseResponse.from(expenses.addExpense(owner(jwt), toEntity(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace an owned expense")
    public ExpenseResponse update(@PathVariable Long id, @Valid @RequestBody ExpenseRequest request,
                                  @AuthenticationPrincipal Jwt jwt) {
        return ExpenseResponse.from(expenses.updateExpense(owner(jwt), id, toEntity(request)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an owned expense")
    public void delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        expenses.deleteExpense(owner(jwt), id);
    }

    private User owner(Jwt jwt) { return identities.resolve(jwt); }

    private static Expense toEntity(ExpenseRequest request) {
        return new Expense(request.amount(), request.category(), request.date(), request.description());
    }
}
