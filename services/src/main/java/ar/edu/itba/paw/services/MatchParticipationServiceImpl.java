package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.PendingJoinRequest;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserLanguages;
import ar.edu.itba.paw.models.types.EventJoinPolicy;
import ar.edu.itba.paw.models.types.EventStatus;
import ar.edu.itba.paw.models.types.EventVisibility;
import ar.edu.itba.paw.persistence.MatchDao;
import ar.edu.itba.paw.persistence.MatchParticipantDao;
import ar.edu.itba.paw.services.exceptions.MatchParticipationException;
import ar.edu.itba.paw.services.mail.MailContent;
import ar.edu.itba.paw.services.mail.MailDispatchService;
import ar.edu.itba.paw.services.mail.MatchLifecycleMailTemplateData;
import ar.edu.itba.paw.services.mail.ThymeleafMailTemplateRenderer;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MatchParticipationServiceImpl implements MatchParticipationService {

    private final MatchDao matchDao;
    private final MatchParticipantDao matchParticipantDao;
    private final UserService userService;
    private final Clock clock;
    private final MailDispatchService mailDispatchService;
    private final ThymeleafMailTemplateRenderer templateRenderer;
    private final MessageSource messageSource;
    private final MatchNotificationService matchNotificationService;

    public MatchParticipationServiceImpl(
            final MatchDao matchDao,
            final MatchParticipantDao matchParticipantDao,
            final UserService userService,
            final Clock clock) {
        this(
                matchDao,
                matchParticipantDao,
                userService,
                clock,
                (recipientEmail, content) -> {},
                null,
                new StaticMessageSource(),
                null);
    }

    @Autowired
    public MatchParticipationServiceImpl(
            final MatchDao matchDao,
            final MatchParticipantDao matchParticipantDao,
            final UserService userService,
            final Clock clock,
            final MailDispatchService mailDispatchService,
            final ThymeleafMailTemplateRenderer templateRenderer,
            final MessageSource messageSource,
            final MatchNotificationService matchNotificationService) {
        this.matchDao = matchDao;
        this.matchParticipantDao = matchParticipantDao;
        this.userService = userService;
        this.clock = clock;
        this.mailDispatchService = mailDispatchService;
        this.templateRenderer = templateRenderer;
        this.messageSource = messageSource;
        this.matchNotificationService = matchNotificationService;
    }

    // -------------------------------------------------------------------------
    // Player actions
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void requestToJoin(final Long matchId, final User user) {
        final Match match = requireMatch(matchId);
        if (user == null) {
            throw new MatchParticipationException(
                    "unauthenticated",
                    "You must be logged in to request to join a recurring event.");
        }

        if (isHost(match, user.getId())) {
            throw new MatchParticipationException(
                    "is_host", "Hosts cannot request to join their own event.");
        }

        if (!EventStatus.OPEN.equals(match.getStatus())) {
            throw new MatchParticipationException(
                    "closed", "The event is not open for join requests.");
        }

        if (match.getVisibility() != EventVisibility.PUBLIC
                || match.getJoinPolicy() != EventJoinPolicy.APPROVAL_REQUIRED) {
            throw new MatchParticipationException(
                    "not_invite_only", "This event does not require approval to join.");
        }

        if (!match.getStartsAt().isAfter(Instant.now(clock))) {
            throw new MatchParticipationException("started", "The event has already started.");
        }

        if (matchParticipantDao.hasActiveReservation(matchId, user)) {
            throw new MatchParticipationException(
                    "already_joined", "You are already a confirmed participant.");
        }

        if (matchParticipantDao.hasPendingRequest(matchId, user)) {
            throw new MatchParticipationException(
                    "already_pending", "You already have a pending join request for this event.");
        }

        if (match.getJoinedPlayers() >= match.getMaxPlayers()) {
            throw new MatchParticipationException("full", "The event is already full.");
        }

        if (!matchParticipantDao.createJoinRequest(matchId, user)) {
            throw new MatchParticipationException(
                    "already_pending", "You already have a pending join request for this event.");
        }

        matchNotificationService.notifyHostJoinRequestReceived(match, user);
    }

    @Override
    @Transactional
    public void requestToJoinSeries(final Long matchId, final User user) {
        final Match match = requireMatch(matchId);
        if (user == null) {
            throw new MatchParticipationException(
                    "unauthenticated",
                    "You must be logged in to request to join a recurring event.");
        }

        if (isHost(match, user.getId())) {
            throw new MatchParticipationException(
                    "is_host", "Hosts cannot request to join their own recurring event series.");
        }

        if (!match.isRecurringOccurrence()) {
            throw new MatchParticipationException(
                    "not_recurring", "The event is not a recurring event.");
        }

        final List<Match> occurrences = matchDao.findSeriesOccurrences(match.getSeries().getId());
        final Instant now = Instant.now(clock);
        final Set<Long> activeFutureReservationMatchIds =
                Set.copyOf(
                        matchParticipantDao.findActiveFutureReservationMatchIdsForSeries(
                                match.getSeries().getId(), user, now));
        final Set<Long> pendingFutureRequestMatchIds =
                Set.copyOf(
                        matchParticipantDao.findPendingFutureRequestMatchIdsForSeries(
                                match.getSeries().getId(), user, now));
        final SeriesJoinRequestEvaluation evaluation =
                evaluateSeriesJoinRequestTargets(
                        occurrences,
                        activeFutureReservationMatchIds,
                        pendingFutureRequestMatchIds,
                        now);
        if (matchParticipantDao.hasPendingSeriesRequest(match.getSeries().getId(), user)) {
            throw buildSeriesJoinRequestFailure(evaluation.asPending());
        }
        if (evaluation.requestableOccurrenceCount() == 0) {
            throw buildSeriesJoinRequestFailure(evaluation);
        }

        final Long requestMatchId =
                pendingFutureRequestMatchIds.contains(matchId)
                        ? matchId
                        : evaluation.targetMatchIds().get(0);
        if (!matchParticipantDao.createSeriesJoinRequestIfSpace(requestMatchId, user)) {
            final Instant currentNow = Instant.now(clock);
            final SeriesJoinRequestEvaluation currentEvaluation =
                    evaluateSeriesJoinRequestTargets(
                            matchDao.findSeriesOccurrences(match.getSeries().getId()),
                            Set.copyOf(
                                    matchParticipantDao
                                            .findActiveFutureReservationMatchIdsForSeries(
                                                    match.getSeries().getId(), user, currentNow)),
                            Set.copyOf(
                                    matchParticipantDao.findPendingFutureRequestMatchIdsForSeries(
                                            match.getSeries().getId(), user, currentNow)),
                            currentNow);
            throw buildSeriesJoinRequestFailure(currentEvaluation);
        }

        matchNotificationService.notifyHostJoinRequestReceived(match, user);
    }

    @Override
    @Transactional
    public void cancelJoinRequest(final Long matchId, final User user) {
        if (!matchParticipantDao.hasPendingRequest(matchId, user)) {
            throw new MatchParticipationException(
                    "no_pending_request", "No pending join request found for this event.");
        }

        if (!matchParticipantDao.cancelJoinRequest(matchId, user)) {
            throw new MatchParticipationException(
                    "no_pending_request", "No pending join request found for this event.");
        }
    }

    @Override
    public boolean hasPendingRequest(final Long matchId, final User user) {
        return matchParticipantDao.hasPendingRequest(matchId, user);
    }

    @Override
    public boolean hasPendingSeriesRequest(final Long matchId, final User user) {
        final Match match = requireMatch(matchId);
        return match.isRecurringOccurrence()
                && matchParticipantDao.hasPendingSeriesRequest(match.getSeries().getId(), user);
    }

    @Override
    public Set<Long> findPendingFutureRequestMatchIdsForSeries(
            final Long seriesId, final User user) {
        if (seriesId == null || user == null) {
            return Set.of();
        }
        return Set.copyOf(
                matchParticipantDao.findPendingFutureRequestMatchIdsForSeries(
                        seriesId, user, Instant.now(clock)));
    }

    // -------------------------------------------------------------------------
    // Host actions
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void approveRequest(final Long matchId, final User host, final User targetUser) {
        final Match match = requireMatch(matchId);
        nonNullUser(host);
        nonNullUser(targetUser);
        requireHost(match, host.getId());

        if (match.isRecurringOccurrence()
                && matchParticipantDao.isSeriesJoinRequest(matchId, targetUser)) {
            final int approvedRows =
                    matchParticipantDao.approveSeriesJoinRequest(
                            match.getSeries().getId(), targetUser, Instant.now(clock));
            if (approvedRows <= 0) {
                throw new MatchParticipationException(
                        "full", "The event is full; cannot approve more participants.");
            }
            matchNotificationService.notifyPlayerRequestApproved(match, targetUser);
            return;
        }

        if (!EventStatus.OPEN.equals(match.getStatus())) {
            throw new MatchParticipationException("closed", "The event is not open.");
        }

        if (match.getJoinedPlayers() >= match.getMaxPlayers()) {
            throw new MatchParticipationException(
                    "full", "The event is full; cannot approve more participants.");
        }

        if (!matchParticipantDao.approveRequest(matchId, targetUser)) {
            throw new MatchParticipationException(
                    "no_pending_request", "No pending join request found for the specified user.");
        }

        matchNotificationService.notifyPlayerRequestApproved(match, targetUser);
    }

    @Override
    @Transactional
    public void rejectRequest(final Long matchId, final User host, final User targetUser) {
        final Match match = requireMatch(matchId);
        nonNullUser(host);
        nonNullUser(targetUser);
        requireHost(match, host.getId());

        if (!matchParticipantDao.rejectRequest(matchId, targetUser)) {
            throw new MatchParticipationException(
                    "no_pending_request", "No pending join request found for the specified user.");
        }

        matchNotificationService.notifyPlayerRequestRejected(match, targetUser);
    }

    @Override
    @Transactional
    public void removeParticipant(final Long matchId, final User host, final User targetUser) {
        final Match match = requireMatch(matchId);
        nonNullUser(host);
        nonNullUser(targetUser);

        if (host.getId().equals(targetUser.getId())) {
            leaveMatch(match, targetUser);
            return;
        }

        requireHost(match, host.getId());

        if (!matchParticipantDao.removeParticipant(matchId, targetUser)) {
            throw new MatchParticipationException(
                    "not_participant",
                    "The specified user is not a confirmed participant of this event.");
        }

        matchNotificationService.notifyPlayerRemovedByHost(match, targetUser);
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    @Override
    public List<User> findPendingRequests(final Long matchId, final User host) {
        final Match match = requireMatch(matchId);
        nonNullUser(host);
        requireHost(match, host.getId());
        return matchParticipantDao.findPendingRequests(matchId);
    }

    @Override
    public List<PendingJoinRequest> findPendingRequestsForHost(final User host) {
        nonNullUser(host);
        return matchParticipantDao.findPendingRequestsForHost(host);
    }

    @Override
    public List<User> findConfirmedParticipants(final Long matchId, final User host) {
        final Match match = requireMatch(matchId);
        nonNullUser(host);
        requireHost(match, host.getId());
        return matchParticipantDao.findConfirmedParticipants(matchId);
    }

    @Override
    public List<Match> findPendingRequestMatches(final User user) {
        if (user == null) {
            throw new MatchParticipationException("invalid_user", "User must be specified");
        }
        final Long userId = user.getId();
        return matchParticipantDao.findPendingMatchIds(user).stream()
                .map(matchDao::findMatchById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(match -> !isHost(match, userId))
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Invite-only flow
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void inviteUser(final Long matchId, final User host, final String email) {
        inviteUser(matchId, host, email, false);
    }

    @Override
    @Transactional
    public void inviteUser(
            final Long matchId, final User host, final String email, final boolean includeSeries) {
        final Match match = requireMatch(matchId);
        nonNullUser(host);
        requireHost(match, host.getId());

        final User target =
                userService
                        .findByEmail(email)
                        .orElseThrow(
                                () ->
                                        new MatchParticipationException(
                                                "user_not_found",
                                                "No user found with that email address."));

        if (isHost(match, target.getId())) {
            throw new MatchParticipationException(
                    "is_host", "The host user cannot be invited to their own match.");
        }

        requireInvitableMatch(match);

        if (includeSeries && match.isRecurringOccurrence()) {
            inviteUserToSeries(match, target);
            return;
        }

        inviteUserToMatch(match, target);
    }

    private void inviteUserToMatch(final Match match, final User target) {
        final Long matchId = match.getId();
        nonNullUser(target);
        if (matchParticipantDao.hasActiveReservation(matchId, target)) {
            throw new MatchParticipationException(
                    "already_joined", "That user is already a confirmed participant.");
        }

        if (matchParticipantDao.hasInvitation(matchId, target)) {
            throw new MatchParticipationException(
                    "already_invited", "That user already has a pending invitation.");
        }

        if (match.getJoinedPlayers() >= match.getMaxPlayers()) {
            throw new MatchParticipationException("full", "The event is already full.");
        }

        if (!matchParticipantDao.inviteUser(matchId, target)) {
            throw new MatchParticipationException(
                    "already_invited", "Could not send the invitation.");
        }

        dispatchMatchInvitation(target, match);
    }

    private void inviteUserToSeries(final Match match, final User target) {
        final List<Match> occurrences = matchDao.findSeriesOccurrences(match.getSeries().getId());
        final SeriesInvitationEvaluation evaluation =
                evaluateSeriesInvitationTargets(occurrences, target);
        if (evaluation.invitableOccurrenceCount() == 0) {
            throw buildSeriesInvitationFailure(evaluation);
        }

        int invitedCount = 0;
        for (final Long targetMatchId : evaluation.targetMatchIds()) {
            if (matchParticipantDao.inviteUser(targetMatchId, target, true)) {
                invitedCount++;
            }
        }

        if (invitedCount == 0) {
            throw new MatchParticipationException(
                    "series_already_covered",
                    "That user is already invited or participating in this series.");
        }

        dispatchSeriesInvitation(target, match, invitedCount);
    }

    @Override
    @Transactional
    public void acceptInvite(final Long matchId, final User user) {
        final Match match = requireMatch(matchId);
        nonNullUser(user);
        final Instant now = Instant.now(clock);

        if (isHost(match, user.getId())) {
            throw new MatchParticipationException(
                    "is_host", "The host cannot accept an invitation to their own event.");
        }

        if (!matchParticipantDao.hasInvitation(matchId, user)) {
            throw new MatchParticipationException(
                    "no_invitation", "No pending invitation found for this event.");
        }

        if (match.isRecurringOccurrence()
                && matchParticipantDao.isSeriesInvitation(matchId, user)) {
            final int acceptedRows =
                    matchParticipantDao.acceptSeriesInvite(match.getSeries().getId(), user, now);
            if (acceptedRows <= 0) {
                throw new MatchParticipationException(
                        "no_invitation", "No pending invitation found for this series.");
            }
            matchNotificationService.notifyHostInviteAccepted(match, user);
            return;
        }

        if (!EventStatus.OPEN.equals(match.getStatus())) {
            throw new MatchParticipationException("closed", "The event is not open.");
        }

        if (!match.getStartsAt().isAfter(now)) {
            throw new MatchParticipationException("started", "The event has already started.");
        }

        if (!matchParticipantDao.acceptInvite(matchId, user)) {
            throw new MatchParticipationException(
                    "no_invitation", "No pending invitation found for this event.");
        }

        matchNotificationService.notifyHostInviteAccepted(match, user);
    }

    @Override
    @Transactional
    public void declineInvite(final Long matchId, final User user) {
        final Match match = requireMatch(matchId);
        nonNullUser(user);

        if (!matchParticipantDao.hasInvitation(matchId, user)) {
            throw new MatchParticipationException(
                    "no_invitation", "No pending invitation found for this event.");
        }

        if (match.isRecurringOccurrence()
                && matchParticipantDao.isSeriesInvitation(matchId, user)) {
            final int declinedRows =
                    matchParticipantDao.declineSeriesInvite(match.getSeries().getId(), user);
            if (declinedRows <= 0) {
                throw new MatchParticipationException(
                        "no_invitation", "No pending invitation found for this series.");
            }
            matchNotificationService.notifyHostInviteDeclined(match, user);
            return;
        }

        if (!matchParticipantDao.declineInvite(matchId, user)) {
            throw new MatchParticipationException(
                    "no_invitation", "No pending invitation found for this event.");
        }

        matchNotificationService.notifyHostInviteDeclined(match, user);
    }

    @Override
    public boolean hasInvitation(final Long matchId, final User user) {
        return matchParticipantDao.hasInvitation(matchId, user);
    }

    @Override
    public boolean isSeriesInvitation(final Long matchId, final User user) {
        return matchParticipantDao.isSeriesInvitation(matchId, user);
    }

    @Override
    public List<User> findInvitedUsers(final Long matchId, final User host) {
        final Match match = requireMatch(matchId);
        nonNullUser(host);
        requireHost(match, host.getId());
        return matchParticipantDao.findInvitedUsers(matchId);
    }

    @Override
    public List<User> findDeclinedInvitees(final Long matchId, final User host) {
        final Match match = requireMatch(matchId);
        nonNullUser(host);
        requireHost(match, host.getId());
        return matchParticipantDao.findDeclinedInvitees(matchId);
    }

    @Override
    public List<Match> findInvitedMatches(final User user) {
        nonNullUser(user);
        return matchParticipantDao.findInvitedMatchIds(user).stream()
                .map(matchDao::findMatchById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(match -> !isHost(match, user.getId()))
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Match requireMatch(final Long matchId) {
        return matchDao.findMatchById(matchId)
                .orElseThrow(
                        () ->
                                new MatchParticipationException(
                                        "not_found", "The event does not exist."));
    }

    private void requireHost(final Match match, final Long userId) {
        if (!isHost(match, userId)) {
            throw new MatchParticipationException(
                    "forbidden", "Only the host can perform this action.");
        }
    }

    private static boolean isHost(final Match match, final Long userId) {
        return userId != null && userId.equals(match.getHost().getId());
    }

    private static void requireInvitableMatch(final Match match) {
        if (!EventStatus.OPEN.equals(match.getStatus())) {
            throw new MatchParticipationException("closed", "The event is not open.");
        }

        if (match.getVisibility() != EventVisibility.PRIVATE
                || match.getJoinPolicy() != EventJoinPolicy.INVITE_ONLY) {
            throw new MatchParticipationException(
                    "not_invite_only",
                    "Invitations are only supported for private invite-only events.");
        }
    }

    private void dispatchMatchInvitation(final User target, final Match match) {
        if (templateRenderer == null) {
            return;
        }
        final MatchLifecycleMailTemplateData templateData =
                buildInvitationTemplateData(target, match);
        final MailContent content =
                templateRenderer.renderMatchInvitationNotification(templateData);
        mailDispatchService.dispatch(target.getEmail(), content);
    }

    private void dispatchSeriesInvitation(
            final User target, final Match match, final int occurrenceCount) {
        if (templateRenderer == null) {
            return;
        }
        final MatchLifecycleMailTemplateData templateData =
                buildInvitationTemplateData(target, match);
        final MailContent content =
                templateRenderer.renderSeriesInvitationNotification(templateData, occurrenceCount);
        mailDispatchService.dispatch(target.getEmail(), content);
    }

    private MatchLifecycleMailTemplateData buildInvitationTemplateData(
            final User recipient, final Match match) {
        final Locale locale = UserLanguages.toLocale(recipient.getPreferredLanguage());
        final String sportLabel =
                messageSource.getMessage(
                        "sport." + match.getSport().getDbValue(),
                        null,
                        match.getSport().getDisplayName(),
                        locale);
        final String statusLabel =
                messageSource.getMessage(
                        "match.status." + match.getStatus().getValue(),
                        null,
                        match.getStatus().getValue(),
                        locale);

        return new MatchLifecycleMailTemplateData(
                recipient.getEmail(),
                match.getTitle(),
                match.getAddress(),
                match.getStartsAt(),
                match.getEndsAt(),
                sportLabel,
                statusLabel,
                locale);
    }

    private void leaveMatch(final Match match, final User user) {
        nonNullUser(user);

        if (!match.getStartsAt().isAfter(Instant.now(clock))) {
            throw new MatchParticipationException("started", "The event has already started.");
        }

        if (!EventStatus.OPEN.equals(match.getStatus())) {
            throw new MatchParticipationException(
                    "not_cancellable", "This reservation can no longer be cancelled.");
        }

        if (!matchParticipantDao.hasActiveReservation(match.getId(), user)) {
            throw new MatchParticipationException(
                    "not_joined", "This account does not have an active reservation.");
        }

        if (!matchParticipantDao.removeParticipant(match.getId(), user)) {
            throw new MatchParticipationException(
                    "not_cancellable", "This reservation can no longer be cancelled.");
        }

        if (isHost(match, user.getId())) {
            return;
        }

        matchNotificationService.notifyHostPlayerLeft(match, user);
    }

    private SeriesJoinRequestEvaluation evaluateSeriesJoinRequestTargets(
            final List<Match> occurrences,
            final Set<Long> activeFutureReservationMatchIds,
            final Set<Long> pendingFutureRequestMatchIds,
            final Instant now) {
        int futureOccurrenceCount = 0;
        int futureOpenApprovalOccurrenceCount = 0;
        int joinedFutureOpenApprovalOccurrenceCount = 0;
        int pendingFutureOpenApprovalOccurrenceCount = 0;
        final java.util.ArrayList<Long> targetMatchIds = new java.util.ArrayList<>();
        int fullOccurrenceCount = 0;

        for (final Match occurrence : occurrences) {
            if (!occurrence.getStartsAt().isAfter(now)) {
                continue;
            }

            futureOccurrenceCount++;
            if (!isSeriesJoinRequestableOccurrence(occurrence)) {
                continue;
            }

            futureOpenApprovalOccurrenceCount++;
            if (activeFutureReservationMatchIds.contains(occurrence.getId())) {
                joinedFutureOpenApprovalOccurrenceCount++;
                continue;
            }

            if (pendingFutureRequestMatchIds.contains(occurrence.getId())) {
                pendingFutureOpenApprovalOccurrenceCount++;
                continue;
            }

            if (occurrence.getJoinedPlayers() >= occurrence.getMaxPlayers()) {
                fullOccurrenceCount++;
                continue;
            }

            targetMatchIds.add(occurrence.getId());
        }

        final boolean joined =
                futureOpenApprovalOccurrenceCount > 0
                        && joinedFutureOpenApprovalOccurrenceCount
                                == futureOpenApprovalOccurrenceCount;
        final boolean pending =
                futureOpenApprovalOccurrenceCount > 0
                        && pendingFutureOpenApprovalOccurrenceCount
                                == futureOpenApprovalOccurrenceCount;
        final boolean alreadyCovered =
                futureOpenApprovalOccurrenceCount > 0
                        && joinedFutureOpenApprovalOccurrenceCount
                                        + pendingFutureOpenApprovalOccurrenceCount
                                == futureOpenApprovalOccurrenceCount;
        return new SeriesJoinRequestEvaluation(
                futureOccurrenceCount,
                futureOpenApprovalOccurrenceCount,
                joined,
                pending || alreadyCovered,
                List.copyOf(targetMatchIds),
                fullOccurrenceCount);
    }

    private static boolean isSeriesJoinRequestableOccurrence(final Match occurrence) {
        return EventStatus.OPEN.equals(occurrence.getStatus())
                && occurrence.getVisibility() == EventVisibility.PUBLIC
                && occurrence.getJoinPolicy() == EventJoinPolicy.APPROVAL_REQUIRED;
    }

    private SeriesInvitationEvaluation evaluateSeriesInvitationTargets(
            final List<Match> occurrences, final User user) {
        int futureOccurrenceCount = 0;
        int futureOpenInviteOnlyOccurrenceCount = 0;
        int joinedFutureOpenInviteOnlyOccurrenceCount = 0;
        int invitedFutureOpenInviteOnlyOccurrenceCount = 0;
        int fullOccurrenceCount = 0;
        final ArrayList<Long> targetMatchIds = new java.util.ArrayList<>();
        final Instant now = Instant.now(clock);

        nonNullUser(user);

        for (final Match occurrence : occurrences) {
            if (!occurrence.getStartsAt().isAfter(now)) {
                continue;
            }

            futureOccurrenceCount++;
            if (!isSeriesInvitableOccurrence(occurrence)) {
                continue;
            }

            futureOpenInviteOnlyOccurrenceCount++;
            if (matchParticipantDao.hasActiveReservation(occurrence.getId(), user)) {
                joinedFutureOpenInviteOnlyOccurrenceCount++;
                continue;
            }

            if (matchParticipantDao.hasInvitation(occurrence.getId(), user)) {
                invitedFutureOpenInviteOnlyOccurrenceCount++;
                continue;
            }

            if (occurrence.getJoinedPlayers() >= occurrence.getMaxPlayers()) {
                fullOccurrenceCount++;
                continue;
            }

            targetMatchIds.add(occurrence.getId());
        }

        return new SeriesInvitationEvaluation(
                futureOccurrenceCount,
                futureOpenInviteOnlyOccurrenceCount,
                joinedFutureOpenInviteOnlyOccurrenceCount,
                invitedFutureOpenInviteOnlyOccurrenceCount,
                List.copyOf(targetMatchIds),
                fullOccurrenceCount);
    }

    private static boolean isSeriesInvitableOccurrence(final Match occurrence) {
        return EventStatus.OPEN.equals(occurrence.getStatus())
                && occurrence.getVisibility() == EventVisibility.PRIVATE
                && occurrence.getJoinPolicy() == EventJoinPolicy.INVITE_ONLY;
    }

    private static void nonNullUser(final User user) {
        if (user == null) {
            throw new MatchParticipationException("invalid_user", "User must be specified.");
        }
    }

    private static MatchParticipationException buildSeriesJoinRequestFailure(
            final SeriesJoinRequestEvaluation evaluation) {
        if (evaluation.futureOccurrenceCount() == 0) {
            return new MatchParticipationException(
                    "series_started", "There are no upcoming dates left in this recurring event.");
        }

        if (evaluation.futureOpenApprovalOccurrenceCount() == 0) {
            return new MatchParticipationException(
                    "series_closed", "The upcoming recurring dates are not open.");
        }

        if (evaluation.joined()) {
            return new MatchParticipationException(
                    "series_already_joined",
                    "You are already confirmed for the future recurring dates.");
        }

        if (evaluation.pending()) {
            return new MatchParticipationException(
                    "series_already_pending",
                    "You already have pending join requests for the future recurring dates.");
        }

        if (evaluation.fullOccurrenceCount() > 0) {
            return new MatchParticipationException(
                    "series_full", "The available future recurring dates are full.");
        }

        return new MatchParticipationException(
                "series_closed", "The upcoming recurring dates are not open.");
    }

    private static MatchParticipationException buildSeriesInvitationFailure(
            final SeriesInvitationEvaluation evaluation) {
        if (evaluation.futureOccurrenceCount() == 0) {
            return new MatchParticipationException(
                    "series_started", "There are no upcoming dates left in this series.");
        }

        if (evaluation.futureOpenInviteOnlyOccurrenceCount() == 0) {
            return new MatchParticipationException(
                    "series_closed", "The upcoming dates in this series are not open.");
        }

        if (evaluation.joined()) {
            return new MatchParticipationException(
                    "series_already_joined",
                    "That user is already participating in every available date in this series.");
        }

        if (evaluation.invited()) {
            return new MatchParticipationException(
                    "series_already_invited",
                    "That user is already invited to every available date in this series.");
        }

        if (evaluation.covered()) {
            return new MatchParticipationException(
                    "series_already_covered",
                    "That user is already invited or participating in this series.");
        }

        if (evaluation.fullOccurrenceCount() > 0) {
            return new MatchParticipationException(
                    "series_full", "The available dates in this series are full.");
        }

        return new MatchParticipationException(
                "series_closed", "The upcoming dates in this series are not open.");
    }

    private record SeriesJoinRequestEvaluation(
            int futureOccurrenceCount,
            int futureOpenApprovalOccurrenceCount,
            boolean joined,
            boolean pending,
            List<Long> targetMatchIds,
            int fullOccurrenceCount) {

        private int requestableOccurrenceCount() {
            return targetMatchIds.size();
        }

        private SeriesJoinRequestEvaluation asPending() {
            return new SeriesJoinRequestEvaluation(
                    futureOccurrenceCount,
                    futureOpenApprovalOccurrenceCount,
                    joined,
                    true,
                    targetMatchIds,
                    fullOccurrenceCount);
        }
    }

    private record SeriesInvitationEvaluation(
            int futureOccurrenceCount,
            int futureOpenInviteOnlyOccurrenceCount,
            int joinedFutureOpenInviteOnlyOccurrenceCount,
            int invitedFutureOpenInviteOnlyOccurrenceCount,
            List<Long> targetMatchIds,
            int fullOccurrenceCount) {

        private int invitableOccurrenceCount() {
            return targetMatchIds.size();
        }

        private boolean joined() {
            return futureOpenInviteOnlyOccurrenceCount > 0
                    && joinedFutureOpenInviteOnlyOccurrenceCount
                            == futureOpenInviteOnlyOccurrenceCount;
        }

        private boolean invited() {
            return futureOpenInviteOnlyOccurrenceCount > 0
                    && invitedFutureOpenInviteOnlyOccurrenceCount
                            == futureOpenInviteOnlyOccurrenceCount;
        }

        private boolean covered() {
            return futureOpenInviteOnlyOccurrenceCount > 0
                    && joinedFutureOpenInviteOnlyOccurrenceCount
                                    + invitedFutureOpenInviteOnlyOccurrenceCount
                            == futureOpenInviteOnlyOccurrenceCount;
        }
    }
}
