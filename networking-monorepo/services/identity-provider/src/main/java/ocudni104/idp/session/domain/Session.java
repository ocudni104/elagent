package ocudni104.idp.session.domain;

import java.time.Clock;
import java.time.Instant;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.Accessors;
import ocudni104.idp.device.domain.DeviceId;
import ocudni104.idp.tenant.domain.TenantId;
import ocudni104.idp.user.domain.UserId;

import java.util.Set;


@Value
@Accessors(fluent = true)
public class Session {
    SessionId id;
    UserId userId;
    TenantId tenantId;
    DeviceId deviceId;
    Set<String> roles;
    Set<String> scopes;
    Instant createdAt;
    Instant absoluteExpiresAt;
    Instant revokedAt;

    @Builder(toBuilder = true)
    private Session(
            SessionId id,
            UserId userId,
            TenantId tenantId,
            DeviceId deviceId,
            @Singular("role") Set<String> roles,
            @Singular("scope") Set<String> scopes,
            Instant createdAt,
            Instant absoluteExpiresAt,
            Instant revokedAt
    ) {
        this.id = java.util.Objects.requireNonNull(id);
        this.userId = java.util.Objects.requireNonNull(userId);
        this.tenantId = tenantId;
        this.deviceId = deviceId;
        this.roles = roles == null ? Set.of() : Set.copyOf(roles);
        this.scopes = scopes == null ? Set.of() : Set.copyOf(scopes);
        this.createdAt = java.util.Objects.requireNonNull(createdAt);
        this.absoluteExpiresAt = java.util.Objects.requireNonNull(absoluteExpiresAt);
        this.revokedAt = revokedAt;

        if (!absoluteExpiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("absoluteExpiresAt must be after createdAt");
        }
    }


    public boolean revoked() {
        return revokedAt() != null;
    }

    public boolean expired(Clock clock) {
        return !absoluteExpiresAt().isAfter(clock.instant());
    }

    public boolean active(Clock clock) {
        return !revoked() && !expired(clock);
    }
}
