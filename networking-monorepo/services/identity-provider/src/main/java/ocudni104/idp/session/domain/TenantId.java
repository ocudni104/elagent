package ocudni104.idp.session.domain;

import java.util.Objects;
import java.util.UUID;

public record TenantId(UUID value) {
    public TenantId {
        Objects.requireNonNull(value);
    }
}
