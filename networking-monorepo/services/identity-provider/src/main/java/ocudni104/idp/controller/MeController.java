package ocudni104.idp.controller;

import jakarta.servlet.http.HttpServletRequest;
import ocudni104.idp.device.domain.DeviceRepository;
import ocudni104.idp.session.application.ValidateSessionUseCase;
import ocudni104.idp.session.domain.Session;
import ocudni104.idp.session.domain.SessionId;
import ocudni104.idp.user.domain.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class MeController {

    private final DeviceRepository deviceRepository;
    private final ValidateSessionUseCase validateSessionUseCase;
    private final UserRepository userRepository;

    @Value("${auth.session.cookie-name:sid}")
    private String sessionCookieName;

    public MeController(
            DeviceRepository deviceRepository,
            ValidateSessionUseCase validateSessionUseCase,
            UserRepository userRepository
    ) {
        this.deviceRepository = deviceRepository;
        this.validateSessionUseCase = validateSessionUseCase;
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(HttpServletRequest request) {
        String rawSessionId = readSessionId(request);
        if (rawSessionId == null) {
            return ResponseEntity.status(401).build();
        }

        Session session = validateSessionUseCase.execute(new SessionId(UUID.fromString(rawSessionId)));
        var user = userRepository.findById(session.userId())
                .orElseThrow(() -> new IllegalStateException("User not found for session " + session.id().value()));
        var device = session.deviceId() == null ? null : deviceRepository.findById(session.deviceId()).orElse(null);

        return ResponseEntity.ok(new MeResponse(
                session.userId().value().toString(),
                session.id().value().toString(),
                user.email(),
                session.tenantId() == null ? null : session.tenantId().value().toString(),
                session.deviceId() == null ? null : session.deviceId().value().toString(),
                device == null ? null : device.os(),
                device == null ? null : device.screen(),
                List.copyOf(session.roles()),
                List.copyOf(session.scopes())
        ));
    }

    private String readSessionId(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (var cookie : request.getCookies()) {
            if (sessionCookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public record MeResponse(
            String userId,
            String sessionId,
            String email,
            String tenantId,
            String deviceId,
            String deviceOs,
            String deviceScreen,
            List<String> roles,
            List<String> scopes
    ) {}
}
