package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.PendingJoinRequest;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.MatchDao;
import ar.edu.itba.paw.persistence.MatchParticipantDao;
import ar.edu.itba.paw.services.exceptions.MatchParticipationException;
import ar.edu.itba.paw.services.mail.MailContent;
import ar.edu.itba.paw.services.mail.MailDispatchService;
import ar.edu.itba.paw.services.mail.MatchLifecycleMailTemplateData;
import ar.edu.itba.paw.services.mail.ThymeleafMailTemplateRenderer;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchParticipationServiceImpl implements MatchParticipationService {

    private final MatchDao matchDao;
    private final MatchParticipantDao matchParticipantDao;
    private final UserService userService;
    private final Clock clock;
    private final MailDispatchService mailDispatchService;
    private final ThymeleafMailTemplateRenderer templateRenderer;
    private final MessageSource messageSource;

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
                new StaticMessageSource());
    }

    @Autowired
    public MatchParticipationServiceImpl(
            final MatchDao matchDao,
            final MatchParticipantDao matchParticipantDao,
            final UserService userService,
            final Clock clock,
            final MailDispatchService mailDispatchService,
            final ThymeleafMailTemplateRenderer templateRenderer,
            final MessageSource messageSource) {
        this.matchDao = matchDao;
        this.matchParticipantDao = matchParticipantDao;
        this.userService = userService;
        this.clock = clock;
        this.mailDispatchService = mailDispatchService;
        this.templateRenderer = templateRenderer;
        this.messageSource = messageSource;
    }

    // -------------------------------------------------------------------------
    // Player actions
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void requestToJoin(final Long matchId, final Long userId) {
        final Match match = requireMatch(matchId);

        if (isHost(match, userId)) {
            throw new MatchParticipationException(
                    "is_host", "Hosts cannot request to join their own event.");
        }

        if (!"open".equalsIgnoreCase(match.getStatus())) {
            throw new MatchParticipationException(
                    "closed", "The event is not open for join requests.");
        }

        if (!"public".equalsIgnoreCase(match.getVisibility())
                || !"approval_required".equalsIgnoreCase(match.getJoinPolicy())) {
            throw new MatchParticipationException(
                    "not_invite_only", "This event does not require approval to join.");
        }

        if (!match.getStartsAt().isAfter(Instant.now(clock))) {
            throw new MatchParticipationException("started", "The event has already started.");
        }

        if (matchParticipantDao.hasActiveReservation(matchId, userId)) {
            throw new MatchParticipationException(
                    "already_joined", "You are already a confirmed participant.");
        }

        if (matchParticipantDao.hasPendingRequest(matchId, userId)) {
            throw new MatchParticipationException(
                    "already_pending", "You already have a pending join request for this event.");
        }

        if (match.getJoinedPlayers() >= match.getMaxPlayers()) {
            throw new MatchParticipationException("full", "The event is already full.");
        }

        if (!matchParticipantDao.createJoinRequest(matchId, userId)) {
            throw new MatchParticipationException(
                    "already_pending", "You already have a pending join request for this event.");
        }
    }

    @Override
    @Transactional
    public void requestToJoinSeries(final Long matchId, final Long userId) {
        final Match match = requireMatch(matchId);

        if (isHost(match, userId)) {
            throw new MatchParticipationException(
                    "is_host", "Hosts cannot request to join their own recurring event series.");
        }

        if (!match.isRecurringOccurrence()) {
            throw new MatchParticipationException(
                    "not_recurring", "The event is not a recurring event.");
        }

        final List<Match> occurrences = matchDao.findSeriesOccurrences(match.getSeriesId());
        final Instant now = Instant.now(clock);
        final Set<Long> activeFutureReservationMatchIds =
                Set.copyOf(
                        matchParticipantDao.findActiveFutureReservationMatchIdsForSeries(
                                match.getSeriesId(), userId, now));
        final Set<Long> pendingFutureRequestMatchIds =
                Set.copyOf(
                        matchParticipantDao.findPendingFutureRequestMatchIdsForSeries(
                                match.getSeriesId(), userId, now));
        final SeriesJoinRequestEvaluation evaluation =
                evaluateSeriesJoinRequestTargets(
                        occurrences,
                        activeFutureReservationMatchIds,
                        pendingFutureRequestMatchIds,
                        now);
        if (matchParticipantDao.hasPendingSeriesRequest(match.getSeriesId(), userId)) {
            throw buildSeriesJoinRequestFailure(evaluation.asPending());
        }
        if (evaluation.requestableOccurrenceCount() == 0) {
            throw buildSeriesJoinRequestFailure(evaluation);
        }

        final Long requestMatchId =
                pendingFutureRequestMatchIds.contains(matchId)
                        ? matchId
                        : evaluation.targetMatchIds().get(0);
        if (!matchParticipantDao.createSeriesJoinRequestIfSpace(requestMatchId, userId)) {
            final Instant currentNow = Instant.now(clock);
            final SeriesJoinRequestEvaluation currentEvaluation =
                    evaluateSeriesJoinRequestTargets(
                            matchDao.findSeriesOccurrences(match.getSeriesId()),
                            Set.copyOf(
                                    matchParticipantDao
                                            .findActiveFutureReservationMatchIdsForSeries(
                                                    match.getSeriesId(), userId, currentNow)),
                            Set.copyOf(
                                    matchParticipantDao.findPendingFutureRequestMatchIdsForSeries(
                                            match.getSeriesId(), userId, currentNow)),
                            currentNow);
            throw buildSeriesJoinRequestFailure(currentEvaluation);
        }
    }

    @Override
    @Transactional
    public void cancelJoinRequest(final Long matchId, final Long userId) {
        if (!matchParticipantDao.hasPendingRequest(matchId, userId)) {
            throw new MatchParticipationException(
                    "no_pending_request", "No pending join request found for this event.");
        }

        if (!matchParticipantDao.cancelJoinRequest(matchId, userId)) {
            throw new MatchParticipationException(
                    "no_pending_request", "No pending join request found for this event.");
        }
    }

    @Override
    public boolean hasPendingRequest(final Long matchId, final Long userId) {
        return matchParticipantDao.hasPendingRequest(matchId, userId);
    }

    @Override
    public boolean hasPendingSeriesRequest(final Long matchId, final Long userId) {
        final Match match = requireMatch(matchId);
        return match.isRecurringOccurrence()
                && matchParticipantDao.hasPendingSeriesRequest(match.getSeriesId(), userId);
    }

    @Override
    public Set<Long> findPendingFutureRequestMatchIdsForSeries(
            final Long seriesId, final Long userId) {
        if (seriesId == null || userId == null) {
            return Set.of();
        }
        return Set.copyOf(
                matchParticipantDao.findPendingFutureRequestMatchIdsForSeries(
                        seriesId, userId, Instant.now(clock)));
    }

    // -------------------------------------------------------------------------
    // Host actions
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void approveRequest(final Long matchId, final Long hostUserId, final Long targetUserId) {
        final Match match = requireMatch(matchId);
        requireHost(match, hostUserId);

        if (match.isRecurringOccurrence()
                && matchParticipantDao.isSeriesJoinRequest(matchId, targetUserId)) {
            final int approvedRows =
                    matchParticipantDao.approveSeriesJoinRequest(
                            match.getSeriesId(), targetUserId, Instant.now(clock));
            if (approvedRows <= 0) {
                throw new MatchParticipationException(
                        "full", "The event is full; cannot approve more participants.");
            }
            return;
        }

        if (!"open".equalsIgnoreCase(match.getStatus())) {
            throw new MatchParticipationException("closed", "The event is not open.");
        }

        if (match.getJoinedPlayers() >= match.getMaxPlayers()) {
            throw new MatchParticipationException(
                    "full", "The event is full; cannot approve more participants.");
        }

        if (!matchParticipantDao.approveRequest(matchId, targetUserId)) {
            throw new MatchParticipationException(
                    "no_pending_request", "No pending join request found for the specified user.");
        }
    }

    @Override
    @Transactional
    public void rejectRequest(final Long matchId, final Long hostUserId, final Long targetUserId) {
        final Match match = requireMatch(matchId);
        requireHost(match, hostUserId);

        if (!matchParticipantDao.rejectRequest(matchId, targetUserId)) {
            throw new MatchParticipationException(
                    "no_pending_request", "No pending join request found for the specified user.");
        }
    }

    @Override
    @Transactional
    public void removeParticipant(
            final Long matchId, final Long hostUserId, final Long targetUserId) {
        final Match match = requireMatch(matchId);

        if (hostUserId.equals(targetUserId)) {
            leaveMatch(match, targetUserId);
            return;
        }

        requireHost(match, hostUserId);

        if (!matchParticipantDao.removeParticipant(matchId, targetUserId)) {
            throw new MatchParticipationException(
                    "not_participant",
                    "The specified user is not a confirmed participant of this event.");
        }
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    @Override
    public List<User> findPendingRequests(final Long matchId, final Long hostUserId) {
        final Match match = requireMatch(matchId);
        requireHost(match, hostUserId);
        return matchParticipantDao.findPendingRequests(matchId);
    }

    @Override
    public List<PendingJoinRequest> findPendingRequestsForHost(final Long hostUserId) {
        return matchParticipantDao.findPendingRequestsForHost(hostUserId);
    }

    @Override
    public List<User> findConfirmedParticipants(final Long matchId, final Long hostUserId) {
        final Match match = requireMatch(matchId);
        requireHost(match, hostUserId);
        return matchParticipantDao.findConfirmedParticipants(matchId);
    }

    @Override
    public List<Match> findPendingRequestMatches(final Long userId) {
        return matchParticipantDao.findPendingMatchIds(userId).stream()
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
    public void inviteUser(final Long matchId, final Long hostUserId, final String email) {
        inviteUser(matchId, hostUserId, email, false);
    }

    @Override
    @Transactional
    public void inviteUser(
            final Long matchId,
            final Long hostUserId,
            final String email,
            final boolean includeSeries) {
        final Match match = requireMatch(matchId);
        requireHost(match, hostUserId);

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

        if (includeSeries && match.getSeriesId() != null) {
            inviteUserToSeries(match, target);
            return;
        }

        inviteUserToMatch(match, target);
    }

    private void inviteUserToMatch(final Match match, final User target) {
        final Long matchId = match.getId();
        if (matchParticipantDao.hasActiveReservation(matchId, target.getId())) {
            throw new MatchParticipationException(
                    "already_joined", "That user is already a confirmed participant.");
        }

        if (matchParticipantDao.hasInvitation(matchId, target.getId())) {
            throw new MatchParticipationException(
                    "already_invited", "That user already has a pending invitation.");
        }

        if (match.getJoinedPlayers() >= match.getMaxPlayers()) {
            throw new MatchParticipationException("full", "The event is already full.");
        }

        if (!matchParticipantDao.inviteUser(matchId, target.getId())) {
            throw new MatchParticipationException(
                    "already_invited", "Could not send the invitation.");
        }

        dispatchMatchInvitation(target, match);
    }

    private void inviteUserToSeries(final Match match, final User target) {
        final List<Match> occurrences = matchDao.findSeriesOccurrences(match.getSeriesId());
        final SeriesInvitationEvaluation evaluation =
                evaluateSeriesInvitationTargets(occurrences, target.getId());
        if (evaluation.invitableOccurrenceCount() == 0) {
            throw buildSeriesInvitationFailure(evaluation);
        }

        int invitedCount = 0;
        for (final Long targetMatchId : evaluation.targetMatchIds()) {
            if (matchParticipantDao.inviteUser(targetMatchId, target.getId(), true)) {
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
    public void acceptInvite(final Long matchId, final Long userId) {
        final Match match = requireMatch(matchId);
        final Instant now = Instant.now(clock);

        if (isHost(match, userId)) {
            throw new MatchParticipationException(
                    "is_host", "The host cannot accept an invitation to their own event.");
        }

        if (!matchParticipantDao.hasInvitation(matchId, userId)) {
            throw new MatchParticipationException(
                    "no_invitation", "No pending invitation found for this event.");
        }

        if (match.getSeriesId() != null
                && matchParticipantDao.isSeriesInvitation(matchId, userId)) {
            final int acceptedRows =
                    matchParticipantDao.acceptSeriesInvite(match.getSeriesId(), userId, now);
            if (acceptedRows <= 0) {
                throw new MatchParticipationException(
                        "no_invitation", "No pending invitation found for this series.");
            }
            return;
        }

        if (!"open".equalsIgnoreCase(match.getStatus())) {
            throw new MatchParticipationException("closed", "The event is not open.");
        }

        if (!match.getStartsAt().isAfter(now)) {
            throw new MatchParticipationException("started", "The event has already started.");
        }

        if (!matchParticipantDao.acceptInvite(matchId, userId)) {
            throw new MatchParticipationException(
                    "no_invitation", "No pending invitation found for this event.");
        }
    }

    @Override
    @Transactional
    public void declineInvite(final Long matchId, final Long userId) {
        final Match match = requireMatch(matchId);

        if (!matchParticipantDao.hasInvitation(matchId, userId)) {
            throw new MatchParticipationException(
                    "no_invitation", "No pending invitation found for this event.");
        }

        if (match.getSeriesId() != null
                && matchParticipantDao.isSeriesInvitation(matchId, userId)) {
            final int declinedRows =
                    matchParticipantDao.declineSeriesInvite(match.getSeriesId(), userId);
            if (declinedRows <= 0) {
                throw new MatchParticipationException(
                        "no_invitation", "No pending invitation found for this series.");
            }
            return;
        }

        if (!matchParticipantDao.declineInvite(matchId, userId)) {
            throw new MatchParticipationException(
                    "no_invitation", "No pending invitation found for this event.");
        }
    }

    @Override
    public boolean hasInvitation(final Long matchId, final Long userId) {
        return matchParticipantDao.hasInvitation(matchId, userId);
    }

    @Override
    public boolean isSeriesInvitation(final Long matchId, final Long userId) {
        return matchParticipantDao.isSeriesInvitation(matchId, userId);
    }

    @Override
    public List<User> findInvitedUsers(final Long matchId, final Long hostUserId) {
        final Match match = requireMatch(matchId);
        requireHost(match, hostUserId);
        return matchParticipantDao.findInvitedUsers(matchId);
    }

    @Override
    public List<User> findDeclinedInvitees(final Long matchId, final Long hostUserId) {
        final Match match = requireMatch(matchId);
        requireHost(match, hostUserId);
        return matchParticipantDao.findDeclinedInvitees(matchId);
    }

    @Override
    public List<Match> findInvitedMatches(final Long userId) {
        return matchParticipantDao.findInvitedMatchIds(userId).stream()
                .map(matchDao::findMatchById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(match -> !isHost(match, userId))
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
        return userId != null && userId.equals(match.getHostUserId());
    }

    private static void requireInvitableMatch(final Match match) {
        if (!"open".equalsIgnoreCase(match.getStatus())) {
            throw new MatchParticipationException("closed", "The event is not open.");
        }

        if (!"private".equalsIgnoreCase(match.getVisibility())
                || !"invite_only".equalsIgnoreCase(match.getJoinPolicy())) {
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
        final Locale locale = LocaleContextHolder.getLocale();
        final String sportLabel =
                messageSource.getMessage(
                        "sport." + match.getSport().getDbValue(),
                        null,
                        match.getSport().getDisplayName(),
                        locale);
        final String statusLabel =
                messageSource.getMessage(
                        "match.status." + match.getStatus(), null, match.getStatus(), locale);

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

    private void leaveMatch(final Match match, final Long userId) {
        if (!match.getStartsAt().isAfter(Instant.now(clock))) {
            throw new MatchParticipationException("started", "The event has already started.");
        }

        if (!"open".equalsIgnoreCase(match.getStatus())) {
            throw new MatchParticipationException(
                    "not_cancellable", "This reservation can no longer be cancelled.");
        }

        if (!matchParticipantDao.hasActiveReservation(match.getId(), userId)) {
            throw new MatchParticipationException(
                    "not_joined", "This account does not have an active reservation.");
        }

        if (!matchParticipantDao.removeParticipant(match.getId(), userId)) {
            throw new MatchParticipationException(
                    "not_cancellable", "This reservation can no longer be cancelled.");
        }
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
        return "open".equalsIgnoreCase(occurrence.getStatus())
                && "public".equalsIgnoreCase(occurrence.getVisibility())
                && "approval_required".equalsIgnoreCase(occurrence.getJoinPolicy());
    }

    private SeriesInvitationEvaluation evaluateSeriesInvitationTargets(
            final List<Match> occurrences, final Long userId) {
        int futureOccurrenceCount = 0;
        int futureOpenInviteOnlyOccurrenceCount = 0;
        int joinedFutureOpenInviteOnlyOccurrenceCount = 0;
        int invitedFutureOpenInviteOnlyOccurrenceCount = 0;
        int fullOccurrenceCount = 0;
        final java.util.ArrayList<Long> targetMatchIds = new java.util.ArrayList<>();
        final Instant now = Instant.now(clock);

        for (final Match occurrence : occurrences) {
            if (!occurrence.getStartsAt().isAfter(now)) {
                continue;
            }

            futureOccurrenceCount++;
            if (!isSeriesInvitableOccurrence(occurrence)) {
                continue;
            }

            futureOpenInviteOnlyOccurrenceCount++;
            if (matchParticipantDao.hasActiveReservation(occurrence.getId(), userId)) {
                joinedFutureOpenInviteOnlyOccurrenceCount++;
                continue;
            }

            if (matchParticipantDao.hasInvitation(occurrence.getId(), userId)) {
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
        return "open".equalsIgnoreCase(occurrence.getStatus())
                && "private".equalsIgnoreCase(occurrence.getVisibility())
                && "invite_only".equalsIgnoreCase(occurrence.getJoinPolicy());
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
