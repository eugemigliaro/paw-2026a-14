package ar.edu.itba.paw.services.exceptions.matchUpdate;

public class MatchUpdatePendingRequestsExceedAvailableException extends MatchUpdateException {
    public MatchUpdatePendingRequestsExceedAvailableException() {
        super("match.update.error.pendingRequestsExceedAvailable");
    }
}
