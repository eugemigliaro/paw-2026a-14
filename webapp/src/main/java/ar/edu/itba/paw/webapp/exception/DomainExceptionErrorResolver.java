package ar.edu.itba.paw.webapp.exception;

import ar.edu.itba.paw.services.exceptions.moderation.ModerationAppealLimitException;
import ar.edu.itba.paw.services.exceptions.moderation.ModerationAppealRejectedException;
import ar.edu.itba.paw.services.exceptions.moderation.ModerationException;
import org.springframework.stereotype.Component;

@Component
public class DomainExceptionErrorResolver {
    public String resolve(ModerationException e) {
        return switch (e) {
            case ModerationAppealLimitException ignored -> "appeal_limit";
            case ModerationAppealRejectedException ignored -> "appeal_rejected";
            default -> e.getMessage();
        };
    }
}
