package ocudni104.idp.device.application;

import lombok.RequiredArgsConstructor;
import ocudni104.idp.device.domain.Device;
import ocudni104.idp.device.domain.DeviceRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class UpsertDeviceUseCase {

    private final DeviceRepository deviceRepository;
    private final Clock clock;

    public void execute(UpsertDeviceCommand command) {
        Instant now = clock.instant();

        Device device = deviceRepository.findById(command.id())
                .map(existing -> existing.toBuilder()
                        .os(command.os() != null ? command.os() : existing.os())
                        .screen(command.screen() != null ? command.screen() : existing.screen())
                        .updatedAt(now)
                        .build())
                .orElseGet(() -> Device.builder()
                        .id(command.id())
                        .os(command.os())
                        .screen(command.screen())
                        .createdAt(now)
                        .updatedAt(now)
                        .build());

        deviceRepository.upsert(device);
    }
}
