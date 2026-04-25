package ocudni104.idp.session.web;

import java.util.Set;
import java.util.UUID;

public record CreateSessionRequest(
    UUID userId, UUID tenantId, UUID deviceId, Set<String> roles, Set<String> scopes) {}
