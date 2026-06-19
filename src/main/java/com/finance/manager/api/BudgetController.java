package com.finance.manager.api;

import com.finance.manager.BudgetConfig;
import com.finance.manager.User;
import com.finance.manager.service.BudgetService;
import com.finance.manager.service.OAuthIdentityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/budget")
@Tag(name = "Budget", description = "One weekly or monthly budget per user")
public class BudgetController {
    private final BudgetService budgets;
    private final OAuthIdentityService identities;

    public BudgetController(BudgetService budgets, OAuthIdentityService identities) {
        this.budgets = budgets;
        this.identities = identities;
    }

    @GetMapping
    @Operation(summary = "Get the authenticated user's budget")
    public BudgetResponse get(@AuthenticationPrincipal Jwt jwt) {
        return BudgetResponse.from(budgets.load(owner(jwt)));
    }

    @PutMapping
    @Operation(summary = "Create or replace the authenticated user's budget")
    public BudgetResponse put(@Valid @RequestBody BudgetRequest request,
                              @AuthenticationPrincipal Jwt jwt) {
        return BudgetResponse.from(budgets.save(owner(jwt),
                new BudgetConfig(request.amount(), request.period())));
    }

    private User owner(Jwt jwt) { return identities.resolve(jwt); }
}
