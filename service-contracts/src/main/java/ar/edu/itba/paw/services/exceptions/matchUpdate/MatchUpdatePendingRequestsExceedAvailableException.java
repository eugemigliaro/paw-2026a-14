package ar.edu.itba.paw.services.exceptions.matchUpdate;

public class MatchUpdatePendingRequestsExceedAvailableException extends MatchUpdateException {

    public MatchUpdatePendingRequestsExceedAvailableException(final String message) {
        super(message);
    }
}
