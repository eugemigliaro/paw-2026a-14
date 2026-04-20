package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.MatchParticipationService;
import ar.edu.itba.paw.services.MatchService;
import ar.edu.itba.paw.services.exceptions.MatchParticipationException;
import ar.edu.itba.paw.webapp.security.AuthenticatedUserPrincipal;
import ar.edu.itba.paw.webapp.security.CurrentAuthenticatedUser;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.PendingRequestViewModel;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.RosterParticipantViewModel;
import ar.edu.itba.paw.webapp.viewmodel.ShellViewModelFactory;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
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

        final ModelAndView mav = new ModelAndView("host/participation/roster");
        mav.addObject("shell", ShellViewModelFactory.hostShell(messageSource, locale));
        mav.addObject("match", match);
        mav.addObject("matchId", resolvedMatchId);
        mav.addObject("participants", toRosterViewModels(participants, resolvedMatchId));
        mav.addObject(
                "emptyMessage",
                messageSource.getMessage("host.roster.empty", null, locale));
        mav.addObject(
                "requestsUrl", "/host/matches/" + resolvedMatchId + "/requests");
        return mav;
    }

    @GetMapping("/host/matches/{matchId}/requests")
    public ModelAndView showPendingRequests(
            @PathVariable("matchId") final String matchId,
            final Locale locale) {
        final long hostUserId = requireAuthenticatedUserId();
        final long resolvedMatchId = parseMatchIdOrThrow(matchId);
        final Match match = requireHostMatch(resolvedMatchId, hostUserId);

        final List<User> pending =
                matchParticipationService.findPendingRequests(resolvedMatchId, hostUserId);

        final ModelAndView mav = new ModelAndView("host/participation/requests");
        mav.addObject("shell", ShellViewModelFactory.hostShell(messageSource, locale));
        mav.addObject("match", match);
        mav.addObject("matchId", resolvedMatchId);
        mav.addObject("pendingRequests", toPendingRequestViewModels(pending, resolvedMatchId));
        mav.addObject(
                "emptyMessage",
                messageSource.getMessage("host.requests.empty", null, locale));
        mav.addObject(
                "rosterUrl", "/host/matches/" + resolvedMatchId + "/participants");
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

        try {
            matchParticipationService.rejectRequest(resolvedMatchId, hostUserId, targetUserId);
            return new ModelAndView(
                    "redirect:/host/matches/" + resolvedMatchId + "/requests?action=rejected");
        } catch (final MatchParticipationException e) {
            return new ModelAndView(
                    "redirect:/host/matches/" + resolvedMatchId + "/requests?error=" + e.getCode());
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
                                        "/host/matches/" + matchId + "/participants/" + u.getId() + "/remove"))
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
                                        "/host/matches/" + matchId + "/requests/" + u.getId() + "/approve",
                                        "/host/matches/" + matchId + "/requests/" + u.getId() + "/reject"))
                .toList();
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
