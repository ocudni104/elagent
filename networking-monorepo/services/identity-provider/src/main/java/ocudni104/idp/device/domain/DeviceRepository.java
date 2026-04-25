package ocudni104.idp.device.domain;

import java.util.Optional;

public interface DeviceRepository {
  Optional<Device> findById(DeviceId id);

  void upsert(Device device);
}
