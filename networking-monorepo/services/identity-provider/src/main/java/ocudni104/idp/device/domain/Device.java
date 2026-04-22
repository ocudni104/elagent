package ocudni104.idp.device.domain;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

import java.time.Instant;

@Value
@Accessors(fluent = true)
public class Device {
    DeviceId id;
    String os;
    String screen;
    Instant createdAt;
    Instant updatedAt;

    @Builder(toBuilder = true)
    private Device(
            DeviceId id,
            String os,
            String screen,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = java.util.Objects.requireNonNull(id);
        this.os = normalize(os);
        this.screen = normalize(screen);
        this.createdAt = java.util.Objects.requireNonNull(createdAt);
        this.updatedAt = java.util.Objects.requireNonNull(updatedAt);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
