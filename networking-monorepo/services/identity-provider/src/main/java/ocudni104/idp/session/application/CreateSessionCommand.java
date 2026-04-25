package ocudni104.idp.session.application;

import java.util.Set;
import ocudni104.idp.device.domain.DeviceId;
import ocudni104.idp.tenant.domain.TenantId;
import ocudni104.idp.user.domain.UserId;

public record CreateSessionCommand(
    UserId userId, TenantId tenantId, DeviceId deviceId, Set<String> roles, Set<String> scopes) {}
