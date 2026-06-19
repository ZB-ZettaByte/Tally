package com.finance.manager.api;

import com.finance.manager.config.ApiSecurityConfig;
import com.finance.manager.service.ExpenseService;
import com.finance.manager.service.OAuthIdentityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExpenseController.class)
@Import(ApiSecurityConfig.class)
class ApiSecurityTest {
    @Autowired MockMvc mvc;
    @MockBean ExpenseService expenses;
    @MockBean OAuthIdentityService identities;
    @MockBean JwtDecoder jwtDecoder;

    @Test
    void missingTokenIsUnauthorized() throws Exception {
        mvc.perform(get("/api/expenses")).andExpect(status().isUnauthorized());
    }

    @Test
    void wrongScopeIsForbidden() throws Exception {
        mvc.perform(get("/api/expenses").with(jwt()
                        .authorities(new SimpleGrantedAuthority("SCOPE_budget.read"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void readScopeAllowsExpenseListing() throws Exception {
        when(expenses.getAllExpenses(null)).thenReturn(List.of());
        mvc.perform(get("/api/expenses").with(jwt()
                        .authorities(new SimpleGrantedAuthority("SCOPE_expenses.read"))))
                .andExpect(status().isOk());
    }
}
