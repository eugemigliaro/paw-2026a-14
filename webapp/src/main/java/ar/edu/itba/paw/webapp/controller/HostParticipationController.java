package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.PendingJoinRequest;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.MatchParticipationService;
import ar.edu.itba.paw.services.MatchService;
import ar.edu.itba.paw.services.exceptions.MatchParticipationException;
import ar.edu.itba.paw.webapp.form.InviteForm;
import ar.edu.itba.paw.webapp.security.AuthenticatedUserPrincipal;
import ar.edu.itba.paw.webapp.security.CurrentAuthenticatedUser;
import ar.edu.itba.paw.webapp.viewmodel.ShellViewModelFactory;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.InviteParticipantViewModel;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.PendingRequestViewModel;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.RosterParticipantViewModel;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class HostParticipationController {

    private final MatchService matchService;
    private final MatchParticipationService matchParticipationService;
    private final MessageSource messageSource;

    @Autowired
    public HostParticipationController(
            final MatchService matchService,
            final MatchParticipationService matchParticipationService,
            final MessageSource messageSource) {
        this.matchService = matchService;
        this.matchParticipationService = matchParticipationService;
        this.messageSource = messageSource;
    }

    @GetMapping("/host/matches/{matchId}/participants")
    public ModelAndView showRoster(
            @PathVariable("matchId") final String matchId, final Locale locale) {
        final long hostUserId = requireAuthenticatedUserId();
        final long resolvedMatchId = parseMatchIdOrThrow(matchId);
        final Match match = requireHostMatch(resolvedMatchId, hostUserId);

        final List<User> participants =
                matchParticipationService.findConfirmedParticipants(resolvedMatchId, hostUserId);

        final boolean isPrivateEvent = "private".equalsIgnoreCase(match.getVisibility());
        final boolean isApprovalRequired =
                "approval_required".equalsIgnoreCase(match.getJoinPolicy());
        final ModelAndView mav = new ModelAndView("host/participation/roster");
        mav.addObject("shell", ShellViewModelFactory.playerShell(messageSource, locale));
        mav.addObject("match", match);
        addParticipationHeader(mav, match, locale);
        mav.addObject("matchId", resolvedMatchId);
        mav.addObject("participants", toRosterViewModels(participants, resolvedMatchId));
        mav.addObject("emptyMessage", messageSource.getMessage("host.roster.empty", null, locale));
        mav.addObject("isPrivateEvent", isPrivateEvent);
        mav.addObject("isApprovalRequired", isApprovalRequired);
        mav.addObject("requestsUrl", "/host/matches/" + resolvedMatchId + "/requests");
        mav.addObject("invitesUrl", "/host/matches/" + resolvedMatchId + "/invites");
        return mav;
    }

    @GetMapping("/host/matches/{matchId}/requests")
    public ModelAndView showPendingRequests(
            @PathVariable("matchId") final String matchId, final Locale locale) {
        final long hostUserId = requireAuthenticatedUserId();
        final long resolvedMatchId = parseMatchIdOrThrow(matchId);
        final Match match = requireHostMatch(resolvedMatchId, hostUserId);

        if (!"approval_required".equalsIgnoreCase(match.getJoinPolicy())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        final List<User> pending =
                matchParticipationService.findPendingRequests(resolvedMatchId, hostUserId);

        final ModelAndView mav = new ModelAndView("host/participation/requests");
        mav.addObject("shell", ShellViewModelFactory.playerShell(messageSource, locale));
        mav.addObject("match", match);
        addParticipationHeader(mav, match, locale);
        mav.addObject("matchId", resolvedMatchId);
        mav.addObject("pendingRequests", toPendingRequestViewModels(pending, resolvedMatchId));
        mav.addObject(
                "emptyMessage", messageSource.getMessage("host.requests.empty", null, locale));
        mav.addObject("rosterUrl", "/host/matches/" + resolvedMatchId + "/participants");
        return mav;
    }

    @GetMapping("/host/requests")
    public ModelAndView showAllPendingRequests(final Locale locale) {
        final long hostUserId = requireAuthenticatedUserId();
        final List<PendingJoinRequest> pending =
                matchParticipationService.findPendingRequestsForHost(hostUserId);

        final ModelAndView mav = new ModelAndView("host/participation/requests");
        mav.addObject(
                "shell", ShellViewModelFactory.hostShell(messageSource, locale, "/host/requests"));
        mav.addObject("aggregateRequests", true);
        mav.addObject("pendingRequests", toHostPendingRequestViewModels(pending));
        mav.addObject(
                "emptyMessage", messageSource.getMessage("host.requests.all.empty", null, locale));
        mav.addObject("matchesUrl", "/events");
        return mav;
    }

    @PostMapping("/host/matches/{matchId}/requests/{userId}/approve")
    public ModelAndView approveRequest(
            @PathVariable("matchId") final String matchId,
            @PathVariable("userId") final String userId,
            final Locale locale) {
        final long hostUserId = requireAuthenticatedUserId();
        final long resolvedMatchId = parseMatchIdOrThrow(matchId);
        final long targetUserId = parseUserIdOrThrow(userId);
        final Match match = requireHostMatch(resolvedMatchId, hostUserId);

        if (!"approval_required".equalsIgnoreCase(match.getJoinPolicy())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        try {
            matchParticipationService.approveRequest(resolvedMatchId, hostUserId, targetUserId);
            return new ModelAndView(
                    "redirect:/host/matches/" + resolvedMatchId + "/requests?action=approved");
        } catch (final MatchParticipationException e) {
            return new ModelAndView(
                    "redirect:/host/matches/" + resolvedMatchId + "/requests?error=" + e.getCode());
        }
    }

    @PostMapping("/host/matches/{matchId}/requests/{userId}/reject")
    public ModelAndView rejectRequest(
            @PathVariable("matchId") final String matchId,
            @PathVariable("userId") final String userId,
            final Locale locale) {
        final long hostUserId = requireAuthenticatedUserId();
        final long resolvedMatchId = parseMatchIdOrThrow(matchId);
        final long targetUserId = parseUserIdOrThrow(userId);
        final Match match = requireHostMatch(resolvedMatchId, hostUserId);

        if (!"approval_required".equalsIgnoreCase(match.getJoinPolicy())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        try {
            matchParticipationService.rejectRequest(resolvedMatchId, hostUserId, targetUserId);
            return new ModelAndView(
                    "redirect:/host/matches/" + resolvedMatchId + "/requests?action=rejected");
        } catch (final MatchParticipationException e) {
            return new ModelAndView(
                    "redirect:/host/matches/" + resolvedMatchId + "/requests?error=" + e.getCode());
        }
    }

    @ModelAttribute("inviteForm")
    public InviteForm inviteForm() {
        return new InviteForm();
    }

    @GetMapping("/host/matches/{matchId}/invites")
    public ModelAndView showInvitePage(
            @PathVariable("matchId") final String matchId, final Locale locale) {
        final long hostUserId = requireAuthenticatedUserId();
        final long resolvedMatchId = parseMatchIdOrThrow(matchId);
        final Match match = requireHostMatch(resolvedMatchId, hostUserId);

        if (!"invite_only".equalsIgnoreCase(match.getJoinPolicy())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        return buildInviteView(match, resolvedMatchId, hostUserId, new InviteForm(), null, locale);
    }

    @PostMapping("/host/matches/{matchId}/invites")
    public ModelAndView sendInvite(
            @PathVariable("matchId") final String matchId,
            @Valid @ModelAttribute("inviteForm") final InviteForm inviteForm,
            final BindingResult bindingResult,
            final Locale locale) {
        final long hostUserId = requireAuthenticatedUserId();
        final long resolvedMatchId = parseMatchIdOrThrow(matchId);
        final Match match = requireHostMatch(resolvedMatchId, hostUserId);

        if (!"invite_only".equalsIgnoreCase(match.getJoinPolicy())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        if (bindingResult.hasErrors()) {
            return buildInviteView(match, resolvedMatchId, hostUserId, inviteForm, null, locale);
        }

        try {
            final boolean includeSeries =
                    inviteForm.isInviteSeries() && match.getSeriesId() != null;
            matchParticipationService.inviteUser(
                    resolvedMatchId, hostUserId, inviteForm.getEmail(), includeSeries);
            return new ModelAndView(
                    "redirect:/host/matches/"
                            + resolvedMatchId
                            + "/invites?action="
                            + (includeSeries ? "seriesInvited" : "invited"));
        } catch (final MatchParticipationException e) {
            final String errorMsg = inviteErrorMessage(e.getCode(), inviteForm.getEmail(), locale);
            return buildInviteView(
                    match, resolvedMatchId, hostUserId, inviteForm, errorMsg, locale);
        }
    }

    private ModelAndView buildInviteView(
            final Match match,
            final long matchId,
            final long hostUserId,
            final InviteForm form,
            final String inviteError,
            final Locale locale) {
        final List<User> pending = matchParticipationService.findInvitedUsers(matchId, hostUserId);
        final List<User> accepted =
                matchParticipationService.findConfirmedParticipants(matchId, hostUserId);
        final List<User> declined =
                matchParticipationService.findDeclinedInvitees(matchId, hostUserId);

        final ModelAndView mav = new ModelAndView("host/participation/invites");
        mav.addObject("shell", ShellViewModelFactory.playerShell(messageSource, locale));
        mav.addObject("match", match);
        addParticipationHeader(mav, match, locale);
        mav.addObject("matchId", matchId);
        mav.addObject("inviteForm", form);
        mav.addObject("inviteError", inviteError);
        mav.addObject("seriesInviteAvailable", match.getSeriesId() != null);
        mav.addObject("pendingInvites", toInviteParticipantViewModels(pending));
        mav.addObject("acceptedParticipants", toRosterViewModels(accepted, matchId));
        mav.addObject("declinedInvites", toInviteParticipantViewModels(declined));
        mav.addObject("rosterUrl", "/host/matches/" + matchId + "/participants");
        return mav;
    }

    private static void addParticipationHeader(
            final ModelAndView mav, final Match match, final Locale locale) {
        final ZoneId zoneId = ZoneId.systemDefault();
        final ZonedDateTime startsAt = match.getStartsAt().atZone(zoneId);
        final Locale resolvedLocale = locale == null ? Locale.ENGLISH : locale;
        mav.addObject(
                "participationEventDate",
                DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                        .withLocale(resolvedLocale)
                        .format(startsAt));
        mav.addObject(
                "participationEventTime",
                DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                        .withLocale(resolvedLocale)
                        .format(startsAt));
        mav.addObject("participationEventVenue", match.getAddress());
    }

    private String inviteErrorMessage(final String code, final String email, final Locale locale) {
        switch (code) {
            case "user_not_found":
                return messageSource.getMessage(
                        "host.invites.error.userNotFound", new Object[] {email}, locale);
            case "already_joined":
                return messageSource.getMessage("host.invites.error.alreadyJoined", null, locale);
            case "already_invited":
                return messageSource.getMessage("host.invites.error.alreadyInvited", null, locale);
            case "full":
                return messageSource.getMessage("host.invites.error.full", null, locale);
            case "is_host":
                return messageSource.getMessage("host.invites.error.isHost", null, locale);
            case "closed":
                return messageSource.getMessage("host.invites.error.closed", null, locale);
            case "series_started":
                return messageSource.getMessage("host.invites.error.seriesStarted", null, locale);
            case "series_closed":
                return messageSource.getMessage("host.invites.error.seriesClosed", null, locale);
            case "series_already_joined":
                return messageSource.getMessage(
                        "host.invites.error.seriesAlreadyJoined", null, locale);
            case "series_already_invited":
                return messageSource.getMessage(
                        "host.invites.error.seriesAlreadyInvited", null, locale);
            case "series_already_covered":
                return messageSource.getMessage(
                        "host.invites.error.seriesAlreadyCovered", null, locale);
            case "series_full":
                return messageSource.getMessage("host.invites.error.seriesFull", null, locale);
            default:
                return messageSource.getMessage("host.invites.error.generic", null, locale);
        }
    }

    @PostMapping("/host/matches/{matchId}/participants/{userId}/remove")
    public ModelAndView removeParticipant(
            @PathVariable("matchId") final String matchId,
            @PathVariable("userId") final String userId,
            final Locale locale) {
        final long hostUserId = requireAuthenticatedUserId();
        final long resolvedMatchId = parseMatchIdOrThrow(matchId);
        final long targetUserId = parseUserIdOrThrow(userId);

        try {
            matchParticipationService.removeParticipant(resolvedMatchId, hostUserId, targetUserId);
            return new ModelAndView(
                    "redirect:/host/matches/" + resolvedMatchId + "/participants?action=removed");
        } catch (final MatchParticipationException e) {
            return new ModelAndView(
                    "redirect:/host/matches/"
                            + resolvedMatchId
                            + "/participants?error="
                            + e.getCode());
        }
    }

    private List<RosterParticipantViewModel> toRosterViewModels(
            final List<User> participants, final long matchId) {
        return participants.stream()
                .map(
                        u ->
                                new RosterParticipantViewModel(
                                        u.getUsername(),
                                        avatarLabel(u.getUsername()),
                                        "/host/matches/"
                                                + matchId
                                                + "/participants/"
                                                + u.getId()
                                                + "/remove",
                                        profileHrefFor(u)))
                .toList();
    }

    private List<InviteParticipantViewModel> toInviteParticipantViewModels(final List<User> users) {
        return users.stream()
                .map(
                        u ->
                                new InviteParticipantViewModel(
                                        u.getUsername(),
                                        avatarLabel(u.getUsername()),
                                        profileHrefFor(u)))
                .toList();
    }

    private List<PendingRequestViewModel> toPendingRequestViewModels(
            final List<User> users, final long matchId) {
        return users.stream()
                .map(
                        u ->
                                new PendingRequestViewModel(
                                        u.getUsername(),
                                        avatarLabel(u.getUsername()),
                                        "/host/matches/"
                                                + matchId
                                                + "/requests/"
                                                + u.getId()
                                                + "/approve",
                                        "/host/matches/"
                                                + matchId
                                                + "/requests/"
                                                + u.getId()
                                                + "/reject",
                                        profileHrefFor(u)))
                .toList();
    }

    private List<PendingRequestViewModel> toHostPendingRequestViewModels(
            final List<PendingJoinRequest> requests) {
        return requests.stream()
                .map(
                        request -> {
                            final User user = request.getUser();
                            final Match match = request.getMatch();
                            final Long matchId = match.getId();
                            return new PendingRequestViewModel(
                                    user.getUsername(),
                                    avatarLabel(user.getUsername()),
                                    "/host/matches/"
                                            + matchId
                                            + "/requests/"
                                            + user.getId()
                                            + "/approve",
                                    "/host/matches/"
                                            + matchId
                                            + "/requests/"
                                            + user.getId()
                                            + "/reject",
                                    match.getTitle(),
                                    "/matches/" + matchId,
                                    request.isSeriesRequest());
                        })
                .toList();
    }

    private static String profileHrefFor(final User user) {
        return user.getUsername() == null ? null : "/users/" + user.getUsername();
    }

    private Match requireHostMatch(final long matchId, final long hostUserId) {
        final Match match =
                matchService
                        .findMatchById(matchId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!match.getHostUserId().equals(hostUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return match;
    }

    private static long requireAuthenticatedUserId() {
        return CurrentAuthenticatedUser.get()
                .map(AuthenticatedUserPrincipal::getUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private static long parseMatchIdOrThrow(final String raw) {
        try {
            return Long.parseLong(raw);
        } catch (final NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    private static long parseUserIdOrThrow(final String raw) {
        try {
            return Long.parseLong(raw);
        } catch (final NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    private static String avatarLabel(final String username) {
        if (username == null || username.isBlank()) {
            return "?";
        }
        final String[] parts = username.trim().split("[^A-Za-z0-9]+");
        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        }
        final String compact = username.replaceAll("[^A-Za-z0-9]", "");
        if (compact.length() >= 2) {
            return compact.substring(0, 2).toUpperCase();
        }
        return compact.isEmpty() ? "?" : compact.substring(0, 1).toUpperCase();
    }
}
