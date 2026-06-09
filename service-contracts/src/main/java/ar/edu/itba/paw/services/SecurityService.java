package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.User;

public interface SecurityService {

    boolean isAuthenticated();

    User currentUser();

    boolean canActAsAdminMod(User actingUser);

    boolean isHost(Long matchId);

    // Match hosting
    boolean canEditMatch(Long matchId);

    boolean canEditMatchSeries(Long matchId);

    boolean canCancelMatch(Long matchId);

    boolean canCancelMatchSeries(Long matchId);

    // Match participation management
    boolean canViewParticipants(Long matchId);

    boolean canApproveJoinRequests(Long matchId);

    boolean canInviteParticipants(Long matchId);

    boolean canManageParticipants(Long matchId);

    // Tournament hosting
    boolean canEditTournament(Long tournamentId);

    boolean canCloseRegistration(Long tournamentId);

    boolean canManageBracket(Long tournamentId);

    boolean canReportMatchWinner(Long tournamentId);

    // Reporting
    boolean canReportUser(String username);

    boolean canReportReview(Long reviewId);

    boolean canReportMatch(Long matchId);

    // Reviews
    boolean canReviewUser(String username);

    boolean canDeleteReview(String username);

    // Ban appeals
    boolean canAppealBan();

    // User moderation reports
    boolean canViewOwnReport(Long reportId);

    boolean canAppealReport(Long reportId);
}
