package ocudni104.idp.user.domain;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

import java.time.Instant;

@Value
@Accessors(fluent = true)
public class User {
    UserId id;
    String email;
    String provider;
    String providerSubject;
    Instant createdAt;
    Instant lastLoginAt;

    @Builder(toBuilder = true)
    private User(
            UserId id,
            String email,
            String provider,
            String providerSubject,
            Instant createdAt,
            Instant lastLoginAt
    ) {
        this.id = java.util.Objects.requireNonNull(id);
        this.email = requireNotBlank(email, "email");
        this.provider = requireNotBlank(provider, "provider");
        this.providerSubject = requireNotBlank(providerSubject, "providerSubject");
        this.createdAt = java.util.Objects.requireNonNull(createdAt);
        this.lastLoginAt = java.util.Objects.requireNonNull(lastLoginAt);
    }

    private static String requireNotBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
