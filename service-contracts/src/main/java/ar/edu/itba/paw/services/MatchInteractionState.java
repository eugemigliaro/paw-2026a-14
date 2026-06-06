package ar.edu.itba.paw.services;

public class MatchInteractionState {

    private final boolean confirmedParticipant;
    private final boolean pendingJoinRequest;
    private final boolean invitedPlayer;
    private final boolean reservationEnabled;
    private final boolean reservationCancellationEnabled;
    private final boolean seriesReservationEnabled;
    private final boolean seriesReservationJoined;
    private final boolean seriesCancellationEnabled;
    private final boolean joinRequestEnabled;
    private final boolean seriesJoinRequestEnabled;
    private final boolean seriesJoinRequestPending;
    private final boolean reservationRequiresLogin;
    private final boolean seriesReservationRequiresLogin;
    private final boolean seriesJoinRequestRequiresLogin;

    public MatchInteractionState(
            final boolean confirmedParticipant,
            final boolean pendingJoinRequest,
            final boolean invitedPlayer,
            final boolean reservationEnabled,
            final boolean reservationCancellationEnabled,
            final boolean seriesReservationEnabled,
            final boolean seriesReservationJoined,
            final boolean seriesCancellationEnabled,
            final boolean joinRequestEnabled,
            final boolean seriesJoinRequestEnabled,
            final boolean seriesJoinRequestPending,
            final boolean reservationRequiresLogin,
            final boolean seriesReservationRequiresLogin,
            final boolean seriesJoinRequestRequiresLogin) {
        this.confirmedParticipant = confirmedParticipant;
        this.pendingJoinRequest = pendingJoinRequest;
        this.invitedPlayer = invitedPlayer;
        this.reservationEnabled = reservationEnabled;
        this.reservationCancellationEnabled = reservationCancellationEnabled;
        this.seriesReservationEnabled = seriesReservationEnabled;
        this.seriesReservationJoined = seriesReservationJoined;
        this.seriesCancellationEnabled = seriesCancellationEnabled;
        this.joinRequestEnabled = joinRequestEnabled;
        this.seriesJoinRequestEnabled = seriesJoinRequestEnabled;
        this.seriesJoinRequestPending = seriesJoinRequestPending;
        this.reservationRequiresLogin = reservationRequiresLogin;
        this.seriesReservationRequiresLogin = seriesReservationRequiresLogin;
        this.seriesJoinRequestRequiresLogin = seriesJoinRequestRequiresLogin;
    }

    public static MatchInteractionState anonymous() {
        return new MatchInteractionState(
                false, false, false, false, false, false, false, false, false, false, false, true,
                true, true);
    }

    public boolean isConfirmedParticipant() {
        return confirmedParticipant;
    }

    public boolean hasPendingJoinRequest() {
        return pendingJoinRequest;
    }

    public boolean isInvitedPlayer() {
        return invitedPlayer;
    }

    public boolean isReservationEnabled() {
        return reservationEnabled;
    }

    public boolean isReservationCancellationEnabled() {
        return reservationCancellationEnabled;
    }

    public boolean isSeriesReservationEnabled() {
        return seriesReservationEnabled;
    }

    public boolean isSeriesReservationJoined() {
        return seriesReservationJoined;
    }

    public boolean isSeriesCancellationEnabled() {
        return seriesCancellationEnabled;
    }

    public boolean isJoinRequestEnabled() {
        return joinRequestEnabled;
    }

    public boolean isSeriesJoinRequestEnabled() {
        return seriesJoinRequestEnabled;
    }

    public boolean isSeriesJoinRequestPending() {
        return seriesJoinRequestPending;
    }

    public boolean isReservationRequiresLogin() {
        return reservationRequiresLogin;
    }

    public boolean isSeriesReservationRequiresLogin() {
        return seriesReservationRequiresLogin;
    }

    public boolean isSeriesJoinRequestRequiresLogin() {
        return seriesJoinRequestRequiresLogin;
    }
}
