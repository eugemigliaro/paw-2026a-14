package ar.edu.itba.paw.webapp.exception;

import ar.edu.itba.paw.services.exceptions.match.*;
import ar.edu.itba.paw.services.exceptions.match.MatchException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.*;
import ar.edu.itba.paw.services.exceptions.moderation.*;
import ar.edu.itba.paw.services.exceptions.playerReview.*;
import ar.edu.itba.paw.services.exceptions.tournamentBracket.*;
import ar.edu.itba.paw.services.exceptions.tournamentLifecycle.*;
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
            case ModerationDuplicateReportException ignored -> "duplicate_report";
            case ModerationReportLimitException ignored -> "report_limit";
            case ModerationInvalidReportException ignored -> "invalid_report";
            case ModerationSelfReportException ignored -> "self_report";
            case ModerationValueTooLongException ignored -> "value_too_long";
            case ModerationReportFailedException ignored -> "report_failed";
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
            case TournamentRegistrationNotInSoloPoolException ignored -> "notInSoloPool";
            case TournamentRegistrationUnderCapacityException ignored -> "underCapacity";
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
            case TournamentBracketNotReadyForBracketException ignored -> "notReady";
            case TournamentBracketAlreadyGeneratedException ignored -> "alreadyGenerated";
            case TournamentBracketPairingStrategyRequiredException ignored ->
                    "pairingStrategyRequired";
            case TournamentBracketInvalidPairingsException ignored -> "invalidPairings";
            case TournamentBracketUnderCapacityException ignored -> "underCapacity";
            case TournamentBracketMissingMatchScheduleException ignored -> "missingMatchSchedule";
            case TournamentBracketInvalidRoundOrderException ignored -> "invalidRoundOrder";
            default -> e.getMessage();
        };
    }

    public String resolve(TournamentLifecycleException e) {
        return switch (e) {
            case TournamentLifecycleInvalidScheduleException ignored -> "invalidSchedule";
            case TournamentLifecycleInvalidFormatException ignored -> "invalidFormat";
            case TournamentLifecycleNotEditableException ignored -> "notEditable";
            case TournamentLifecycleNotCancellableException ignored -> "notCancellable";
            case TournamentLifecycleInvalidBracketSizeException ignored -> "invalidBracketSize";
            case TournamentLifecycleInvalidTeamSizeException ignored -> "invalidTeamSize";
            case TournamentLifecycleInvalidJoinModeException ignored -> "invalidJoinMode";
            case TournamentLifecycleInvalidRegistrationWindowException ignored ->
                    "invalidRegistrationWindow";
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
