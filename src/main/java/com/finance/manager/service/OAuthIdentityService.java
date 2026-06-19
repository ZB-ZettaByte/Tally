package com.finance.manager.service;

import com.finance.manager.User;
import com.finance.manager.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Maps an immutable OIDC subject to a local data owner on first API access. */
@Service
@Transactional
public class OAuthIdentityService {
    private final UserRepository users;

    public OAuthIdentityService(UserRepository users) {
        this.users = users;
    }

    public User resolve(Jwt jwt) {
        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Access token has no subject claim.");
        }
        return users.findByExternalSubject(subject).orElseGet(() -> provision(jwt));
    }

    private User provision(Jwt jwt) {
        String preferred = firstNonBlank(jwt.getClaimAsString("preferred_username"),
                jwt.getClaimAsString("email"), "oauth-user");
        String safe = preferred.trim().replaceAll("[^A-Za-z0-9._-]", "-");
        String suffix = subjectSuffix(jwt.getSubject());
        String username = users.existsByUsername(safe) ? safe + "-" + suffix : safe;
        try {
            return users.saveAndFlush(User.oauthUser(username, jwt.getSubject()));
        } catch (DataIntegrityViolationException race) {
            return users.findByExternalSubject(jwt.getSubject()).orElseThrow(() -> race);
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value;
        return "oauth-user";
    }

    private static String subjectSuffix(String subject) {
        String compact = subject.replaceAll("[^A-Za-z0-9]", "");
        return compact.substring(0, Math.min(8, compact.length()));
    }
}
