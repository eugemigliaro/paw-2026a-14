package ar.edu.itba.paw.services.security;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.ModerationReport;
import ar.edu.itba.paw.models.PlayerReview;
import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserBan;
import ar.edu.itba.paw.models.types.EventJoinPolicy;
import ar.edu.itba.paw.models.types.EventVisibility;
import ar.edu.itba.paw.models.types.TournamentStatus;
import ar.edu.itba.paw.services.ModerationService;
import ar.edu.itba.paw.services.SecurityService;
import ar.edu.itba.paw.services.internal.MatchDataService;
import ar.edu.itba.paw.services.internal.PlayerReviewDataService;
import ar.edu.itba.paw.services.internal.TournamentDataService;
import ar.edu.itba.paw.services.internal.UserDataService;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("securityService")
@Transactional(readOnly = true)
public class SecurityServiceImpl implements SecurityService {

    private static final String ADMIN_MOD_AUTHORITY = "ROLE_ADMIN_MOD";

    private final MatchDataService matchDataService;
    private final TournamentDataService tournamentDataService;
    private final PlayerReviewDataService playerReviewService;
    private final UserDataService userService;
    private final ModerationService moderationService;

    public SecurityServiceImpl(
            final MatchDataService matchDataService,
            final TournamentDataService tournamentDataService,
            final PlayerReviewDataService playerReviewService,
            final UserDataService userService,
            final ModerationService moderationService) {
        this.matchDataService = matchDataService;
        this.tournamentDataService = tournamentDataService;
        this.playerReviewService = playerReviewService;
        this.userService = userService;
        this.moderationService = moderationService;
    }

    @Override
    public boolean isAuthenticated() {
        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        final Object principal = auth.getPrincipal();
        return principal != null && extractUserFromPrincipal(principal) != null;
    }

    @Override
    public User currentUser() {
        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return null;
        }
        final Object principal = auth.getPrincipal();
        return extractUserFromPrincipal(principal);
    }

    @Override
    public boolean canActAsAdminMod(final User actingUser) {
        if (actingUser == null || actingUser.getId() == null) {
            return false;
        }
        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        final User currentUser = extractUserFromPrincipal(auth.getPrincipal());
        if (currentUser == null || !Objects.equals(currentUser.getId(), actingUser.getId())) {
            return false;
        }
        return auth.getAuthorities().stream()
                .filter(Objects::nonNull)
                .anyMatch(authority -> ADMIN_MOD_AUTHORITY.equals(authority.getAuthority()));
    }

    private User extractUserFromPrincipal(final Object principal) {
        if (principal instanceof AuthenticatedPrincipal authenticatedPrincipal) {
            return authenticatedPrincipal.getAuthenticatedUser();
        }
        return null;
    }

    @Override
    public boolean isHost(final Long matchId) {
        if (matchId == null) {
            return false;
        }
        final User current = currentUser();
        if (current == null) {
            return false;
        }
        final Optional<Match> match = matchDataService.findById(matchId);
        return match.map(m -> current.getId().equals(m.getHost().getId())).orElse(false);
    }

    @Override
    public boolean canEditMatch(final Long matchId) {
        final User current = currentUser();
        if (current == null || matchId == null) {
            return false;
        }
        final Match match = matchDataService.findById(matchId).orElse(null);
        if (match == null) {
            return false;
        }
        return (isHost(match, current) || canActAsAdminMod(current)) && !hasStarted(match);
    }

    @Override
    public boolean canEditMatchSeries(final Long matchId) {
        final User current = currentUser();
        if (current == null || matchId == null) {
            return false;
        }
        final Match match = matchDataService.findById(matchId).orElse(null);
        if (match == null) {
            return false;
        }
        return (isHost(match, current) || canActAsAdminMod(current))
                && !hasStarted(match)
                && match.isRecurringOccurrence();
    }

    @Override
    public boolean canCancelMatch(final Long matchId) {
        final User current = currentUser();
        if (current == null || matchId == null) {
            return false;
        }
        final Match match = matchDataService.findById(matchId).orElse(null);
        if (match == null) {
            return false;
        }
        return isHost(match, current) && !hasStarted(match);
    }

    @Override
    public boolean canCancelMatchSeries(final Long matchId) {
        final User current = currentUser();
        if (current == null || matchId == null) {
            return false;
        }
        final Match match = matchDataService.findById(matchId).orElse(null);
        if (match == null) {
            return false;
        }
        return isHost(match, current) && !hasStarted(match) && match.isRecurringOccurrence();
    }

    @Override
    public boolean canViewParticipants(final Long matchId) {
        if (matchId == null) {
            return false;
        }
        return isHost(matchId) || canActAsAdminMod(currentUser());
    }

    @Override
    public boolean canApproveJoinRequests(final Long matchId) {
        final User current = currentUser();
        if (current == null || matchId == null) {
            return false;
        }
        final Match match = matchDataService.findById(matchId).orElse(null);
        if (match == null) {
            return false;
        }
        return (isHost(match, current) || canActAsAdminMod(current))
                && isApprovalRequired(match)
                && !hasStarted(match);
    }

    @Override
    public boolean canInviteParticipants(final Long matchId) {
        final User current = currentUser();
        if (current == null || matchId == null) {
            return false;
        }
        final Match match = matchDataService.findById(matchId).orElse(null);
        if (match == null) {
            return false;
        }
        return isHost(match, current) && isInviteOnly(match) && !hasStarted(match);
    }

    @Override
    public boolean canManageParticipants(final Long matchId) {
        return canEditMatch(matchId);
    }

    @Override
    public boolean canEditTournament(final Long tournamentId) {
        return validateTournamentAccessWithStatus(tournamentId, TournamentStatus.REGISTRATION);
    }

    @Override
    public boolean canCloseRegistration(final Long tournamentId) {
        return validateTournamentAccessWithStatus(tournamentId, TournamentStatus.REGISTRATION);
    }

    @Override
    public boolean canManageBracket(final Long tournamentId) {
        return validateTournamentAccessWithStatus(tournamentId, TournamentStatus.BRACKET_SETUP);
    }

    @Override
    public boolean canReportMatchWinner(final Long tournamentId) {
        return validateTournamentAccessWithStatus(tournamentId, TournamentStatus.IN_PROGRESS);
    }

    @Override
    public boolean canReportUser(final String username) {
        final User current = currentUser();
        if (current == null || username == null) {
            return false;
        }
        return !username.equals(current.getUsername());
    }

    @Override
    public boolean canReportReview(final Long reviewId) {
        final User current = currentUser();
        if (current == null || reviewId == null) {
            return false;
        }
        final PlayerReview review = playerReviewService.findById(reviewId).orElse(null);
        if (review == null) {
            return false;
        }
        return review.getReviewer() != null
                && !review.getReviewer().getId().equals(current.getId());
    }

    @Override
    public boolean canReportMatch(final Long matchId) {
        final User current = currentUser();
        if (current == null || matchId == null) {
            return false;
        }
        final Match match = matchDataService.findById(matchId).orElse(null);
        if (match == null) {
            return false;
        }
        return !isHost(match, current);
    }

    @Override
    public boolean canReviewUser(final String username) {
        final User current = currentUser();
        if (current == null || username == null) {
            return false;
        }
        final User reviewedUser = userService.findByUsername(username).orElse(null);
        if (reviewedUser == null) {
            return false;
        }
        return !username.equals(current.getUsername())
                && playerReviewService.canReview(current, reviewedUser);
    }

    @Override
    public boolean canDeleteReview(final String username) {
        final User current = currentUser();
        if (current == null || username == null) {
            return false;
        }
        final User reviewedUser = userService.findByUsername(username).orElse(null);
        if (reviewedUser == null) {
            return false;
        }
        final PlayerReview review =
                playerReviewService.findByPair(current, reviewedUser).orElse(null);
        return (review != null
                        && review.getReviewer() != null
                        && review.getReviewer().getId().equals(current.getId()))
                || canActAsAdminMod(current);
    }

    @Override
    public boolean canAppealBan() {
        final User current = currentUser();
        if (current == null) {
            return false;
        }
        final UserBan ban = moderationService.findActiveBan(current).orElse(null);
        if (ban == null) {
            return false;
        }
        return ban != null;
    }

    @Override
    public boolean canViewOwnReport(final Long reportId) {
        final User current = currentUser();
        if (current == null || reportId == null) {
            return false;
        }
        final ModerationReport report = moderationService.findReportById(reportId).orElse(null);
        return report != null
                && report.getReporter() != null
                && report.getReporter().getId().equals(current.getId());
    }

    @Override
    public boolean canAppealReport(final Long reportId) {
        final User current = currentUser();
        if (current == null || reportId == null) {
            return false;
        }
        final ModerationReport report = moderationService.findReportById(reportId).orElse(null);
        return report != null
                && report.getReporter() != null
                && report.getReporter().getId().equals(current.getId())
                && report.getAppealCount() != 0;
    }

    private boolean hasStarted(final Match match) {
        return match.getStartsAt().isBefore(Instant.now());
    }

    private boolean isHost(final Match match, final User user) {
        return match.getHost().getId().equals(user.getId());
    }

    private boolean isHost(final Tournament tournament, final User user) {
        return tournament.getHost().getId().equals(user.getId());
    }

    private boolean isApprovalRequired(final Match match) {
        return match.getJoinPolicy() == EventJoinPolicy.APPROVAL_REQUIRED
                && match.getVisibility() == EventVisibility.PUBLIC;
    }

    private boolean isInviteOnly(final Match match) {
        return match.getJoinPolicy() == EventJoinPolicy.INVITE_ONLY
                && match.getVisibility() == EventVisibility.PRIVATE;
    }

    private boolean validateTournamentAccessWithStatus(
            final Long tournamentId, final TournamentStatus requiredStatus) {
        final User user = currentUser();
        if (user == null || tournamentId == null || requiredStatus == null) {
            return false;
        }
        final Tournament tournament = tournamentDataService.findById(tournamentId).orElse(null);
        if (tournament == null) {
            return false;
        }
        return (isHost(tournament, user) || canActAsAdminMod(user))
                && tournament.getStatus() == requiredStatus;
    }
}
