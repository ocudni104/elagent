package ocudni104.idp.session.domain;

import org.junit.jupiter.api.Test;
import ocudni104.idp.tenant.domain.TenantId;
import ocudni104.idp.user.domain.UserId;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SessionTest {

    private static final Instant CREATED_AT = Instant.parse("2026-04-19T10:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-04-19T11:00:00Z");

    @Test
    void shouldRejectWhenAbsoluteExpiryIsBeforeCreatedAt() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> session(
                CREATED_AT,
                CREATED_AT.minusSeconds(1),
                null
        ));

        assertEquals("absoluteExpiresAt must be after createdAt", ex.getMessage());
    }

    @Test
    void shouldRejectWhenAbsoluteExpiryEqualsCreatedAt() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> session(
                CREATED_AT,
                CREATED_AT,
                null
        ));

        assertEquals("absoluteExpiresAt must be after createdAt", ex.getMessage());
    }

    @Test
    void shouldReportNotRevokedWhenRevokedAtIsNull() {
        assertFalse(session(CREATED_AT, EXPIRES_AT, null).revoked());
    }

    @Test
    void shouldReportRevokedWhenRevokedAtIsPresent() {
        assertTrue(session(CREATED_AT, EXPIRES_AT, CREATED_AT.plusSeconds(30)).revoked());
    }

    @Test
    void shouldReportNotExpiredBeforeAbsoluteExpiry() {
        Session session = session(CREATED_AT, EXPIRES_AT, null);
        Clock clock = Clock.fixed(EXPIRES_AT.minusSeconds(1), ZoneOffset.UTC);

        assertFalse(session.expired(clock));
    }

    @Test
    void shouldReportExpiredAtAbsoluteExpiry() {
        Session session = session(CREATED_AT, EXPIRES_AT, null);
        Clock clock = Clock.fixed(EXPIRES_AT, ZoneOffset.UTC);

        assertTrue(session.expired(clock));
    }

    @Test
    void shouldReportExpiredAfterAbsoluteExpiry() {
        Session session = session(CREATED_AT, EXPIRES_AT, null);
        Clock clock = Clock.fixed(EXPIRES_AT.plusSeconds(1), ZoneOffset.UTC);

        assertTrue(session.expired(clock));
    }

    @Test
    void shouldBeActiveWhenNotRevokedAndNotExpired() {
        Session session = session(CREATED_AT, EXPIRES_AT, null);
        Clock clock = Clock.fixed(EXPIRES_AT.minusSeconds(1), ZoneOffset.UTC);

        assertTrue(session.active(clock));
    }

    @Test
    void shouldNotBeActiveWhenRevoked() {
        Session session = session(CREATED_AT, EXPIRES_AT, CREATED_AT.plusSeconds(10));
        Clock clock = Clock.fixed(EXPIRES_AT.minusSeconds(1), ZoneOffset.UTC);

        assertFalse(session.active(clock));
    }

    @Test
    void shouldNotBeActiveWhenExpired() {
        Session session = session(CREATED_AT, EXPIRES_AT, null);
        Clock clock = Clock.fixed(EXPIRES_AT, ZoneOffset.UTC);

        assertFalse(session.active(clock));
    }

    private static Session session(Instant createdAt, Instant absoluteExpiresAt, Instant revokedAt) {
        return Session.builder()
                .id(SessionId.newId())
                .userId(new UserId(UUID.randomUUID()))
                .tenantId(new TenantId(UUID.randomUUID()))
                .deviceId(new DeviceId(UUID.randomUUID()))
                .roles(Set.of("ROLE_USER"))
                .scopes(Set.of("profile"))
                .createdAt(createdAt)
                .absoluteExpiresAt(absoluteExpiresAt)
                .revokedAt(revokedAt)
                .build();
    }
}
