package ocudni104.idp.device.application;

import ocudni104.idp.device.domain.DeviceId;

public record UpsertDeviceCommand(DeviceId id, String os, String screen) {}
