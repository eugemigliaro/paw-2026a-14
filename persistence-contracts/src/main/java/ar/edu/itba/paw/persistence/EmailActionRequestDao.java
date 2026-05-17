package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.EmailActionRequest;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.types.EmailActionStatus;
import ar.edu.itba.paw.models.types.EmailActionType;
import java.time.Instant;
import java.util.Optional;

public interface EmailActionRequestDao {

    EmailActionRequest create(
            EmailActionType actionType,
            String email,
            User user,
            String tokenHash,
            String payloadJson,
            Instant expiresAt);

    Optional<EmailActionRequest> findByTokenHash(String tokenHash);

    Optional<EmailActionRequest> findByTokenHashForUpdate(String tokenHash);

    void updateStatus(Long id, EmailActionStatus status, User user, Instant consumedAt);

    void expirePendingByEmailAndActionType(
            EmailActionType actionType, String email, Instant consumedAt);
}
