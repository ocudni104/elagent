package ocudni104.idp.session.application;

import lombok.RequiredArgsConstructor;
import ocudni104.idp.session.domain.Session;
import ocudni104.idp.session.domain.SessionRepository;
import ocudni104.idp.session.domain.SessionId;
import ocudni104.idp.session.domain.exception.SessionExpiredException;
import ocudni104.idp.session.domain.exception.SessionNotFoundException;
import ocudni104.idp.session.domain.exception.SessionRevokedException;
import org.springframework.stereotype.Service;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class ValidateSessionUseCase {

    private final SessionRepository sessionRepository;
    private final Clock clock;

    public Session execute(SessionId sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));

        if (session.revoked()) {
            throw new SessionRevokedException(sessionId);
        }

        if (session.expired(clock)) {
            throw new SessionExpiredException(sessionId);
        }

        return session;
    }
}
