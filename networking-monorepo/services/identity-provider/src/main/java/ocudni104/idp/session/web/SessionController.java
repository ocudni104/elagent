package ocudni104.idp.session.web;


import lombok.RequiredArgsConstructor;
import ocudni104.idp.session.application.CreateSessionCommand;
import ocudni104.idp.session.application.CreateSessionUseCase;
import ocudni104.idp.session.application.RevokeSessionUseCase;
import ocudni104.idp.session.application.ValidateSessionUseCase;
import ocudni104.idp.session.domain.DeviceId;
import ocudni104.idp.session.domain.Session;
import ocudni104.idp.session.domain.SessionId;
import ocudni104.idp.session.domain.TenantId;
import ocudni104.idp.session.domain.UserId;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final CreateSessionUseCase createSessionUseCase;
    private final ValidateSessionUseCase validateSessionUseCase;
    private final RevokeSessionUseCase revokeSessionUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SessionResponse create(@RequestBody CreateSessionRequest request) {
        Session session = createSessionUseCase.execute(
                new CreateSessionCommand(
                        new UserId(request.userId()),
                        request.tenantId() == null ? null : new TenantId(request.tenantId()),
                        request.deviceId() == null ? null : new DeviceId(request.deviceId()),
                        request.roles(),
                        request.scopes()
                )
        );

        return SessionResponse.from(session);
    }

    @GetMapping("/{id}/validate")
    public SessionResponse validate(@PathVariable UUID id) {
        return SessionResponse.from(validateSessionUseCase.execute(new SessionId(id)));
    }

    @PostMapping("/{id}/revoke")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable UUID id) {
        revokeSessionUseCase.execute(new SessionId(id));
    }
}
