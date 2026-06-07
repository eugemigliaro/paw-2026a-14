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
            case ModerationAppealLimitException ignored -> "appealLimit";
            case ModerationAppealRejectedException ignored -> "appealRejected";
            case ModerationInvalidBanDurationException ignored -> "invalidBanDuration";
            case ModerationTargetNotFoundException ignored -> "targetNotFound";
            case ModerationReportErrorException ignored -> "reportError";
            case ModerationDuplicateReportException ignored -> "duplicateReport";
            case ModerationReportLimitException ignored -> "reportLimit";
            case ModerationInvalidReportException ignored -> "invalidReport";
            case ModerationSelfReportException ignored -> "selfReport";
            case ModerationValueTooLongException ignored -> "valueTooLong";
            case ModerationReportFailedException ignored -> "reportFailed";
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
            case MatchNotRecurringException ignored -> "notRecurring";
            case MatchSeriesClosedException ignored -> "seriesClosed";
            case MatchSeriesFullException ignored -> "seriesFull";
            case MatchSeriesStartedException ignored -> "seriesStarted";
            case MatchStartedException ignored -> "started";
            default -> e.getMessage();
        };
    }

    public String resolve(MatchParticipationException e) {
        return switch (e) {
            case MatchParticipationAlreadyInvitedException ignored -> "alreadyInvited";
            case MatchParticipationAlreadyJoinedException ignored -> "alreadyJoined";
            case MatchParticipationAlreadyPendingException ignored -> "alreadyPending";
            case MatchParticipationIsHostException ignored -> "isHost";
            case MatchParticipationNoInvitationException ignored -> "noInvitation";
            case MatchParticipationNoPendingRequestException ignored -> "noPendingRequest";
            case MatchParticipationNotCancellableException ignored -> "notCancellable";
            case MatchParticipationNotInviteOnlyException ignored -> "notInviteOnly";
            case MatchParticipationNotJoinedException ignored -> "notJoined";
            case MatchParticipationNotParticipantException ignored -> "notParticipant";
            case MatchParticipationSeriesAlreadyCoveredException ignored -> "seriesAlreadyCovered";
            case MatchParticipationSeriesAlreadyInvitedException ignored -> "seriesAlreadyInvited";
            case MatchParticipationSeriesAlreadyJoinedException ignored -> "seriesAlreadyJoined";
            case MatchParticipationSeriesAlreadyPendingException ignored -> "seriesAlreadyPending";
            case MatchParticipationSeriesNotJoinedException ignored -> "seriesNotJoined";
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
            case PlayerReviewInvalidReactionException ignored -> "invalidReaction";
            case PlayerReviewSelfReviewException ignored -> "selfReview";
            case PlayerReviewNotEligibleException ignored -> "notEligible";
            case PlayerReviewCommentTooLongException ignored -> "commentTooLong";
            case PlayerReviewUserNotFoundException ignored -> "userNotFound";
            default -> e.getMessage();
        };
    }
}
