package ocudni104.idp.device.domain;

import java.util.Objects;
import java.util.UUID;

public record DeviceId(UUID value) {
  public DeviceId {
    Objects.requireNonNull(value);
  }
}
