package ocudni104.idp.session.web;

import ocudni104.idp.session.domain.Session;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record SessionResponse(
        UUID id,
        UUID userId,
        UUID tenantId,
        UUID deviceId,
        Set<String> roles,
        Set<String> scopes,
        Instant createdAt,
        Instant absoluteExpiresAt,
        Instant revokedAt
) {
    public static SessionResponse from(Session session) {
        return new SessionResponse(
                session.id().value(),
                session.userId().value(),
                session.tenantId() == null ? null : session.tenantId().value(),
                session.deviceId() == null ? null : session.deviceId().value(),
                session.roles(),
                session.scopes(),
                session.createdAt(),
                session.absoluteExpiresAt(),
                session.revokedAt()
        );
    }
}
