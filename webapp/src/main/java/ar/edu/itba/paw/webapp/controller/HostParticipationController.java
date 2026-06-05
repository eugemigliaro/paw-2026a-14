package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.PendingJoinRequest;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.types.EventJoinPolicy;
import ar.edu.itba.paw.services.MatchParticipationService;
import ar.edu.itba.paw.services.MatchService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.services.exceptions.MatchParticipationException;
import ar.edu.itba.paw.webapp.form.InviteForm;
import ar.edu.itba.paw.webapp.utils.SecurityControllerUtils;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.PendingRequestViewModel;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class HostParticipationController {

    private final MatchService matchService;
    private final MatchParticipationService matchParticipationService;
    private final UserService userService;
    private final MessageSource messageSource;

    @Autowired
    public HostParticipationController(
            final MatchService matchService,
            final MatchParticipationService matchParticipationService,
            final UserService userService,
            final MessageSource messageSource) {
        this.matchService = matchService;
        this.matchParticipationService = matchParticipationService;
        this.userService = userService;
        this.messageSource = messageSource;
    }

    @GetMapping("/host/matches/{matchId:\\d+}/participants")
    public ModelAndView showRoster(
            @PathVariable("matchId") final Long matchId, final Locale locale) {
        final User host = SecurityControllerUtils.requireAuthenticatedUser();
        try {
            matchParticipationService.findConfirmedParticipants(matchId, host);
        } catch (final MatchParticipationException e) {
            throw participationAccessStatus(e);
        }
        return new ModelAndView("redirect:/matches/" + matchId + "#participants");
    }

    @GetMapping("/host/matches/{matchId:\\d+}/requests")
    public ModelAndView showPendingRequests(
            @PathVariable("matchId") final Long matchId, final Locale locale) {
        final User host = SecurityControllerUtils.requireAuthenticatedUser();
        try {
            matchParticipationService.findPendingRequests(matchId, host);
        } catch (final MatchParticipationException e) {
            throw participationAccessStatus(e);
        }
        final Match match = findMatchOrThrow(matchId);

        if (match.getJoinPolicy()
                != EventJoinPolicy.APPROVAL_REQUIRED) { // TODO: remove business logic
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        return new ModelAndView("redirect:/matches/" + matchId + "#pending-requests");
    }

    @GetMapping("/host/requests")
    public ModelAndView showAllPendingRequests(final Locale locale) {
        final User host = SecurityControllerUtils.requireAuthenticatedUser();
        final List<PendingJoinRequest> pending =
                matchParticipationService.findPendingRequestsForHost(host);

        final ModelAndView mav = new ModelAndView("host/participation/aggregate-requests");
        mav.addObject("aggregateRequests", true);
        mav.addObject("pendingRequests", toHostPendingRequestViewModels(pending));
        mav.addObject(
                "emptyMessage", messageSource.getMessage("host.requests.all.empty", null, locale));
        mav.addObject("matchesUrl", "/events");
        return mav;
    }

    @PostMapping("/host/matches/{matchId:\\d+}/requests/{userId:\\d+}/approve")
    public ModelAndView approveRequest(
            @PathVariable("matchId") final Long matchId,
            @PathVariable("userId") final Long userId,
            final Locale locale,
            final RedirectAttributes redirectAttributes) {
        final User host = SecurityControllerUtils.requireAuthenticatedUser();
        final User user = findUserOrThrow(userId);
        final Match match = findMatchOrThrow(matchId);

        if (match.getJoinPolicy()
                != EventJoinPolicy.APPROVAL_REQUIRED) { // TODO: remove business logic
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        try {
            matchParticipationService.approveRequest(matchId, host, user);
            redirectAttributes.addFlashAttribute("hostAction", "requestApproved");
            return redirectToMatch(matchId);
        } catch (final MatchParticipationException e) {
            throwIfAccessFailure(e);
            redirectAttributes.addFlashAttribute("hostActionTarget", "requests");
            redirectAttributes.addFlashAttribute(
                    "hostActionError", requestErrorMessage(e.getCode(), locale));
            return redirectToMatch(matchId);
        }
    }

    @PostMapping("/host/matches/{matchId:\\d+}/requests/{userId:\\d+}/reject")
    public ModelAndView rejectRequest(
            @PathVariable("matchId") final Long matchId,
            @PathVariable("userId") final Long userId,
            final Locale locale,
            final RedirectAttributes redirectAttributes) {
        final User host = SecurityControllerUtils.requireAuthenticatedUser();
        final Match match = findMatchOrThrow(matchId);
        final User user = findUserOrThrow(userId);

        if (match.getJoinPolicy()
                != EventJoinPolicy.APPROVAL_REQUIRED) { // TODO: remove business logic
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        try {
            matchParticipationService.rejectRequest(matchId, host, user);
            redirectAttributes.addFlashAttribute("hostAction", "requestRejected");
            return redirectToMatch(matchId);
        } catch (final MatchParticipationException e) {
            throwIfAccessFailure(e);
            redirectAttributes.addFlashAttribute("hostActionTarget", "requests");
            redirectAttributes.addFlashAttribute(
                    "hostActionError", requestErrorMessage(e.getCode(), locale));
            return redirectToMatch(matchId);
        }
    }

    @ModelAttribute("inviteForm")
    public InviteForm inviteForm() {
        return new InviteForm();
    }

    @GetMapping("/host/matches/{matchId:\\d+}/invites")
    public ModelAndView showInvitePage(
            @PathVariable("matchId") final Long matchId, final Locale locale) {
        final User host = SecurityControllerUtils.requireAuthenticatedUser();
        try {
            matchParticipationService.findInvitedUsers(matchId, host);
        } catch (final MatchParticipationException e) {
            throw participationAccessStatus(e);
        }
        final Match match = findMatchOrThrow(matchId);

        if (match.getJoinPolicy() != EventJoinPolicy.INVITE_ONLY) { // TODO: remove business logic
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        return new ModelAndView("redirect:/matches/" + matchId + "#pending-invitations");
    }

    @PostMapping("/host/matches/{matchId:\\d+}/invites")
    public ModelAndView sendInvite(
            @PathVariable("matchId") final Long matchId,
            @Valid @ModelAttribute("inviteForm") final InviteForm inviteForm,
            final BindingResult bindingResult,
            final Locale locale,
            final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("hostActionTarget", "invites");
            redirectAttributes.addFlashAttribute("hostInviteEmail", inviteForm.getEmail());
            redirectAttributes.addFlashAttribute(
                    "hostActionError", inviteValidationErrorMessage(bindingResult, locale));
            return redirectToMatch(matchId);
        }

        final User host = SecurityControllerUtils.requireAuthenticatedUser();
        final Match match = findMatchOrThrow(matchId);

        if (match.getJoinPolicy() != EventJoinPolicy.INVITE_ONLY) { // TODO: remove business logic
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        try {
            final boolean includeSeries =
                    inviteForm.isInviteSeries()
                            && match.isRecurringOccurrence(); // TODO: remove business logic
            matchParticipationService.inviteUser(
                    matchId, host, inviteForm.getEmail(), includeSeries);
            redirectAttributes.addFlashAttribute(
                    "hostAction", includeSeries ? "seriesInviteSent" : "inviteSent");
            return redirectToMatch(matchId);
        } catch (final MatchParticipationException e) {
            throwIfAccessFailure(e);
            final String errorMsg = inviteErrorMessage(e.getCode(), inviteForm.getEmail(), locale);
            redirectAttributes.addFlashAttribute("hostActionTarget", "invites");
            redirectAttributes.addFlashAttribute("hostInviteEmail", inviteForm.getEmail());
            redirectAttributes.addFlashAttribute("hostActionError", errorMsg);
            return redirectToMatch(matchId);
        }
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

    @PostMapping("/host/matches/{matchId:\\d+}/participants/{userId:\\d+}/remove")
    public ModelAndView removeParticipant(
            @PathVariable("matchId") final Long matchId,
            @PathVariable("userId") final Long userId,
            final Locale locale,
            final RedirectAttributes redirectAttributes) {
        final User host = SecurityControllerUtils.requireAuthenticatedUser();
        final User user = findUserOrThrow(userId);

        try {
            matchParticipationService.removeParticipant(matchId, host, user);
            redirectAttributes.addFlashAttribute("hostAction", "participantRemoved");
            return redirectToMatch(matchId);
        } catch (final MatchParticipationException e) {
            throwIfAccessFailure(e);
            redirectAttributes.addFlashAttribute("hostActionTarget", "participants");
            redirectAttributes.addFlashAttribute(
                    "hostActionError", participantErrorMessage(e.getCode(), locale));
            return redirectToMatch(matchId);
        }
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

    private Match findMatchOrThrow(final Long matchId) {
        return matchService
                .findMatchById(matchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private User findUserOrThrow(final Long userId) {
        return userService
                .findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private ModelAndView redirectToMatch(final Long matchId) {
        return new ModelAndView("redirect:/matches/" + matchId);
    }

    private void throwIfAccessFailure(final MatchParticipationException e) {
        if ("forbidden".equals(e.getCode()) || "not_found".equals(e.getCode())) {
            throw participationAccessStatus(e);
        }
    }

    private ResponseStatusException participationAccessStatus(final MatchParticipationException e) {
        if ("not_found".equals(e.getCode())) {
            return new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if ("forbidden".equals(e.getCode())) {
            return new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return new ResponseStatusException(HttpStatus.BAD_REQUEST);
    }

    private String inviteValidationErrorMessage(
            final BindingResult bindingResult, final Locale locale) {
        return bindingResult.getAllErrors().stream()
                .findFirst()
                .map(error -> messageSource.getMessage(error, locale))
                .orElseGet(
                        () -> messageSource.getMessage("host.invites.error.generic", null, locale));
    }

    private String requestErrorMessage(final String code, final Locale locale) {
        if ("full".equals(code)) {
            return messageSource.getMessage("event.host.requests.error.full", null, locale);
        }
        if ("closed".equals(code)) {
            return messageSource.getMessage("event.host.requests.error.closed", null, locale);
        }
        return messageSource.getMessage("event.host.requests.error.noPendingRequest", null, locale);
    }

    private String participantErrorMessage(final String code, final Locale locale) {
        switch (code) {
            case "started":
                return messageSource.getMessage(
                        "event.host.participants.error.started", null, locale);
            case "not_cancellable":
                return messageSource.getMessage(
                        "event.host.participants.error.notCancellable", null, locale);
            case "not_joined":
            case "not_participant":
                return messageSource.getMessage(
                        "event.host.participants.error.notParticipant", null, locale);
            default:
                return messageSource.getMessage(
                        "event.host.participants.error.generic", null, locale);
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
