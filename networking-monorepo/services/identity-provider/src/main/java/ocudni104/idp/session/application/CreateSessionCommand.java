package ocudni104.idp.session.application;

import ocudni104.idp.session.domain.DeviceId;
import ocudni104.idp.session.domain.TenantId;
import ocudni104.idp.session.domain.UserId;

import java.util.Set;

public record CreateSessionCommand(
        UserId userId,
        TenantId tenantId,
        DeviceId deviceId,
        Set<String> roles,
        Set<String> scopes
) {}
