package ocudni104.idp.session.domain;

import java.util.Objects;
import java.util.UUID;

public record DeviceId(UUID value) {
    public DeviceId {
        Objects.requireNonNull(value);
    }
}
