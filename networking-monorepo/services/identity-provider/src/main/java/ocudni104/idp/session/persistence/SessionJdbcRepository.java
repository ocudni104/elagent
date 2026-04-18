package ocudni104.idp.session.persistence;

import lombok.RequiredArgsConstructor;
import ocudni104.idp.session.domain.DeviceId;
import ocudni104.idp.session.domain.Session;
import ocudni104.idp.session.domain.SessionRepository;
import ocudni104.idp.session.domain.SessionId;
import ocudni104.idp.session.domain.TenantId;
import ocudni104.idp.session.domain.UserId;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SessionJdbcRepository implements SessionRepository {

    private final JdbcClient jdbc;

    @Override
    public Optional<Session> findById(SessionId id) {
        return jdbc.sql("""
                SELECT
                    id,
                    user_id,
                    tenant_id,
                    device_id,
                    created_at,
                    absolute_expires_at,
                    revoked_at
                FROM sessions
                WHERE id = :id
                """)
                .param("id", id.value())
                .query((rs, rowNum) -> mapSession(rs))
                .optional();
    }

    @Override
    public void save(Session session) {
        jdbc.sql("""
                insert into sessions (
                    id,
                    user_id,
                    tenant_id,
                    device_id,
                    created_at,
                    absolute_expires_at,
                    revoked_at
                ) values (
                    :id,
                    :userId,
                    :tenantId,
                    :deviceId,
                    :createdAt,
                    :absoluteExpiresAt,
                    :revokedAt
                )
            """)
                .param("id", session.id().value())
                .param("userId", session.userId().value())
                .param("tenantId", session.tenantId() == null ? null : session.tenantId().value())
                .param("deviceId", session.deviceId() == null ? null : session.deviceId().value())
                .param("createdAt", Timestamp.from(session.createdAt()))
                .param("absoluteExpiresAt", Timestamp.from(session.absoluteExpiresAt()))
                .param("revokedAt", session.revokedAt() == null ? null : Timestamp.from(session.revokedAt()))
                .update();
    }

    @Override
    public boolean revoke(SessionId id) {
        int updated = jdbc.sql("""
                update sessions
                set revoked_at = current_timestamp
                where id = :id
                  and revoked_at is null
            """)
                .param("id", id.value())
                .update();

        return updated > 0;
    }

    private Session mapSession(ResultSet rs) throws SQLException {
        return Session.builder()
                .id(new SessionId(rs.getObject("id", UUID.class)))
                .userId(new UserId(rs.getObject("user_id", UUID.class)))
                .tenantId(toTenantId(rs, "tenant_id"))
                .deviceId(toDeviceId(rs, "device_id"))
                .roles(Set.of())   // fill later if stored elsewhere
                .scopes(Set.of())  // fill later if stored elsewhere
                .createdAt(toInstant(rs, "created_at"))
                .absoluteExpiresAt(toInstant(rs, "absolute_expires_at"))
                .revokedAt(toInstantNullable(rs, "revoked_at"))
                .build();
    }

    private Instant toInstant(ResultSet rs, String column) throws SQLException {
        return rs.getTimestamp(column).toInstant();
    }

    private Instant toInstantNullable(ResultSet rs, String column) throws SQLException {
        Timestamp ts = rs.getTimestamp(column);
        return ts == null ? null : ts.toInstant();
    }

    private TenantId toTenantId(ResultSet rs, String column) throws SQLException {
        UUID value = rs.getObject(column, UUID.class);
        return value == null ? null : new TenantId(value);
    }

    private DeviceId toDeviceId(ResultSet rs, String column) throws SQLException {
        UUID value = rs.getObject(column, UUID.class);
        return value == null ? null : new DeviceId(value);
    }
}
