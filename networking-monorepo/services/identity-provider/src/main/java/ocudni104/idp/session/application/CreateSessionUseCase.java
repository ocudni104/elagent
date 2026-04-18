package ocudni104.idp.session.application;

import lombok.RequiredArgsConstructor;
import ocudni104.idp.session.domain.SessionId;
import ocudni104.idp.session.domain.Session;
import ocudni104.idp.session.domain.SessionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class CreateSessionUseCase {

    private final SessionRepository sessionRepository;
    private final Clock clock;

    @Value("${auth.session.absolute-ttl}")
    private Duration sessionTtl;

    public Session execute(CreateSessionCommand cmd) {
        Instant now = clock.instant();

        Session session = Session.builder()
                .id(SessionId.newId())
                .userId(cmd.userId())
                .tenantId(cmd.tenantId())
                .deviceId(cmd.deviceId())
                .roles(cmd.roles())
                .scopes(cmd.scopes())
                .createdAt(now)
                .absoluteExpiresAt(now.plus(sessionTtl))
                .build();

        sessionRepository.save(session);
        return session;
    }
}
