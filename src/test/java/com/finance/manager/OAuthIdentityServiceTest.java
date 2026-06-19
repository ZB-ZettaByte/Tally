package com.finance.manager;

import com.finance.manager.repository.UserRepository;
import com.finance.manager.service.OAuthIdentityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(OAuthIdentityService.class)
class OAuthIdentityServiceTest {
    @Autowired OAuthIdentityService identities;
    @Autowired UserRepository users;

    @Test
    void repeatedSubjectResolvesToSameLocalOwner() {
        User first = identities.resolve(jwt("subject-123", "alex"));
        User second = identities.resolve(jwt("subject-123", "renamed-alex"));

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(users.count()).isEqualTo(1);
    }

    @Test
    void usernameCollisionDoesNotMergeDifferentSubjects() {
        User first = identities.resolve(jwt("subject-one", "alex"));
        User second = identities.resolve(jwt("subject-two", "alex"));

        assertThat(second.getId()).isNotEqualTo(first.getId());
        assertThat(second.getUsername()).startsWith("alex-");
    }

    private static Jwt jwt(String subject, String username) {
        Instant now = Instant.now();
        return Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject(subject)
                .claim("preferred_username", username)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .build();
    }
}
