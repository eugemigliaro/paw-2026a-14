package ar.edu.itba.paw.webapp.exception;

import ar.edu.itba.paw.services.exceptions.match.*;
import ar.edu.itba.paw.services.exceptions.match.MatchException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.*;
import ar.edu.itba.paw.services.exceptions.moderation.*;
import ar.edu.itba.paw.services.exceptions.playerReview.*;
import ar.edu.itba.paw.services.exceptions.tournamentBracket.*;
import ar.edu.itba.paw.services.exceptions.tournamentRegistration.*;
import org.springframework.stereotype.Component;

@Component
public class DomainExceptionErrorResolver {
    public String resolve(ModerationException e) {
        return switch (e) {
            case ModerationAppealLimitException ignored -> "appeal_limit";
            case ModerationAppealRejectedException ignored -> "appeal_rejected";
            case ModerationInvalidBanDurationException ignored -> "invalid_ban_duration";
            case ModerationTargetNotFoundException ignored -> "target_not_found";
            case ModerationReportErrorException ignored -> "report_error";
            default -> e.getMessage();
        };
    }

    public String resolve(MatchException e) {
        if (e instanceof MatchParticipationException participationException) {
            return resolve(participationException);
        }
        return switch (e) {
            case MatchClosedException ignored -> "closed";
            case MatchFullException ignored -> "full";
            case MatchNotRecurringException ignored -> "not_recurring";
            case MatchSeriesClosedException ignored -> "series_closed";
            case MatchSeriesFullException ignored -> "series_full";
            case MatchSeriesStartedException ignored -> "series_started";
            case MatchStartedException ignored -> "started";
            default -> e.getMessage();
        };
    }

    public String resolve(MatchParticipationException e) {
        return switch (e) {
            case MatchParticipationAlreadyInvitedException ignored -> "already_invited";
            case MatchParticipationAlreadyJoinedException ignored -> "already_joined";
            case MatchParticipationAlreadyPendingException ignored -> "already_pending";
            case MatchParticipationIsHostException ignored -> "is_host";
            case MatchParticipationNoInvitationException ignored -> "no_invitation";
            case MatchParticipationNoPendingRequestException ignored -> "no_pending_request";
            case MatchParticipationNotCancellableException ignored -> "not_cancellable";
            case MatchParticipationNotInviteOnlyException ignored -> "not_invite_only";
            case MatchParticipationNotJoinedException ignored -> "not_joined";
            case MatchParticipationNotParticipantException ignored -> "not_joined";
            case MatchParticipationSeriesAlreadyCoveredException ignored ->
                    "series_already_covered";
            case MatchParticipationSeriesAlreadyInvitedException ignored ->
                    "series_already_invited";
            case MatchParticipationSeriesAlreadyJoinedException ignored -> "series_already_joined";
            case MatchParticipationSeriesAlreadyPendingException ignored ->
                    "series_already_pending";
            case MatchParticipationSeriesNotJoinedException ignored -> "series_not_joined";
            default -> e.getMessage();
        };
    }

    public String resolve(TournamentRegistrationException e) {
        return switch (e) {
            case TournamentRegistrationNotOpenException ignored -> "notOpen";
            case TournamentRegistrationSoloSignupDisabledException ignored -> "soloDisabled";
            case TournamentRegistrationAlreadyOnTeamException ignored -> "alreadyOnTeam";
            case TournamentRegistrationAlreadyAssignedException ignored -> "alreadyAssigned";
            case TournamentRegistrationSoloPoolFullException ignored -> "soloPoolFull";
            default -> e.getMessage();
        };
    }

    public String resolve(TournamentBracketException e) {
        return switch (e) {
            case TournamentBracketNotGeneratedException ignored -> "notGenerated";
            case TournamentBracketNotInProgressException ignored -> "notInProgress";
            case TournamentBracketMatchNotReadyException ignored -> "matchNotReady";
            case TournamentBracketWinnerNotInMatchException ignored -> "winnerNotInMatch";
            case TournamentBracketMatchAlreadyDecidedException ignored -> "matchAlreadyDecided";
            default -> e.getMessage();
        };
    }

    public String resolve(PlayerReviewException e) {
        return switch (e) {
            case PlayerReviewInvalidReactionException ignored -> "invalid_reaction";
            case PlayerReviewSelfReviewException ignored -> "self_review";
            case PlayerReviewNotEligibleException ignored -> "not_eligible";
            case PlayerReviewCommentTooLongException ignored -> "comment_too_long";
            case PlayerReviewUserNotFoundException ignored -> "user_not_found";
            default -> e.getMessage();
        };
    }
}
