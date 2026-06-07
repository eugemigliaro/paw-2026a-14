package ar.edu.itba.paw.models.exceptions.matchUpdate;

public class MatchUpdatePendingRequestsExceedAvailableException extends MatchUpdateException {
    public MatchUpdatePendingRequestsExceedAvailableException() {
        super("pendingRequestsExceedAvailable");
    }
}
