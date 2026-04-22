package ocudni104.idp.device.persistence;

import lombok.RequiredArgsConstructor;
import ocudni104.idp.device.domain.Device;
import ocudni104.idp.device.domain.DeviceId;
import ocudni104.idp.device.domain.DeviceRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DeviceJdbcRepository implements DeviceRepository {

    private final JdbcClient jdbc;

    @Override
    public Optional<Device> findById(DeviceId id) {
        return jdbc.sql("""
                SELECT id, os, screen, created_at, updated_at
                FROM devices
                WHERE id = :id
                """)
                .param("id", id.value())
                .query((rs, rowNum) -> Device.builder()
                        .id(new DeviceId(rs.getObject("id", UUID.class)))
                        .os(rs.getString("os"))
                        .screen(rs.getString("screen"))
                        .createdAt(rs.getTimestamp("created_at").toInstant())
                        .updatedAt(rs.getTimestamp("updated_at").toInstant())
                        .build())
                .optional();
    }

    @Override
    public void upsert(Device device) {
        jdbc.sql("""
                INSERT INTO devices (id, os, screen, created_at, updated_at)
                VALUES (:id, :os, :screen, :createdAt, :updatedAt)
                ON CONFLICT (id) DO UPDATE
                SET os = EXCLUDED.os,
                    screen = EXCLUDED.screen,
                    updated_at = EXCLUDED.updated_at
                """)
                .param("id", device.id().value())
                .param("os", device.os())
                .param("screen", device.screen())
                .param("createdAt", Timestamp.from(device.createdAt()))
                .param("updatedAt", Timestamp.from(device.updatedAt()))
                .update();
    }
}
