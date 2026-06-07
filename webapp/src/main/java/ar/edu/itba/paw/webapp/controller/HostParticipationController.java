package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.PendingJoinRequest;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.MatchParticipationService;
import ar.edu.itba.paw.services.MatchService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationAlreadyInvitedException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationAlreadyJoinedException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationClosedException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationForbiddenException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationFullException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationIsHostException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationNotCancellableException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationNotFoundException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationNotJoinedException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationNotParticipantException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationSeriesAlreadyCoveredException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationSeriesAlreadyInvitedException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationSeriesAlreadyJoinedException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationSeriesClosedException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationSeriesFullException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationSeriesStartedException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationStartedException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationUserNotFoundException;
import ar.edu.itba.paw.webapp.form.InviteForm;
import ar.edu.itba.paw.webapp.security.annotation.AuthenticatedUser;
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
            @AuthenticatedUser final User user,
            @PathVariable("matchId") final Long matchId,
            final Locale locale) {
        try {
            matchParticipationService.findConfirmedParticipants(matchId, user);
        } catch (final MatchParticipationException e) {
            throw participationAccessStatus(e);
        }
        return new ModelAndView("redirect:/matches/" + matchId + "#participants");
    }

    @GetMapping("/host/matches/{matchId:\\d+}/requests")
    public ModelAndView showPendingRequests(
            @AuthenticatedUser final User user,
            @PathVariable("matchId") final Long matchId,
            final Locale locale) {
        try {
            matchParticipationService.findPendingRequests(matchId, user);
        } catch (final MatchParticipationException e) {
            throw participationAccessStatus(e);
        }
        return new ModelAndView("redirect:/matches/" + matchId + "#pending-requests");
    }

    @GetMapping("/host/requests")
    public ModelAndView showAllPendingRequests(
            @AuthenticatedUser final User user,
            final Locale locale) { // TODO: this method is never used
        final List<PendingJoinRequest> pending =
                matchParticipationService.findPendingRequestsForHost(user);

        final ModelAndView mav = new ModelAndView("host/participation/aggregate-requests");
        mav.addObject("aggregateRequests", true);
        mav.addObject("pendingRequests", pending);
        mav.addObject(
                "emptyMessage", messageSource.getMessage("host.requests.all.empty", null, locale));
        mav.addObject("matchesUrl", "/events");
        return mav;
    }

    @PostMapping("/host/matches/{matchId:\\d+}/requests/{userId:\\d+}/approve")
    public ModelAndView approveRequest(
            @AuthenticatedUser final User user,
            @PathVariable("matchId") final Long matchId,
            @PathVariable("userId") final Long userId,
            final Locale locale,
            final RedirectAttributes redirectAttributes) {
        final User targetUser = findUserOrThrow(userId);

        try {
            matchParticipationService.approveRequest(matchId, user, targetUser);
            redirectAttributes.addFlashAttribute("hostAction", "requestApproved");
            return redirectToMatch(matchId);
        } catch (final MatchParticipationException e) {
            throwIfAccessFailure(e);
            redirectAttributes.addFlashAttribute("hostActionTarget", "requests");
            redirectAttributes.addFlashAttribute("hostActionError", requestErrorMessage(e, locale));
            return redirectToMatch(matchId);
        }
    }

    @PostMapping("/host/matches/{matchId:\\d+}/requests/{userId:\\d+}/reject")
    public ModelAndView rejectRequest(
            @AuthenticatedUser final User user,
            @PathVariable("matchId") final Long matchId,
            @PathVariable("userId") final Long userId,
            final Locale locale,
            final RedirectAttributes redirectAttributes) {
        final User targetUser = findUserOrThrow(userId);

        try {
            matchParticipationService.rejectRequest(matchId, user, targetUser);
            redirectAttributes.addFlashAttribute("hostAction", "requestRejected");
            return redirectToMatch(matchId);
        } catch (final MatchParticipationException e) {
            throwIfAccessFailure(e);
            redirectAttributes.addFlashAttribute("hostActionTarget", "requests");
            redirectAttributes.addFlashAttribute("hostActionError", requestErrorMessage(e, locale));
            return redirectToMatch(matchId);
        }
    }

    @ModelAttribute("inviteForm")
    public InviteForm inviteForm() {
        return new InviteForm();
    }

    @GetMapping("/host/matches/{matchId:\\d+}/invites")
    public ModelAndView showInvitePage(
            @AuthenticatedUser final User user,
            @PathVariable("matchId") final Long matchId,
            final Locale locale) {
        try {
            matchParticipationService.findInvitedUsers(matchId, user);
        } catch (final MatchParticipationException e) {
            throw participationAccessStatus(e);
        }

        return new ModelAndView("redirect:/matches/" + matchId + "#pending-invitations");
    }

    @PostMapping("/host/matches/{matchId:\\d+}/invites")
    public ModelAndView sendInvite(
            @AuthenticatedUser final User user,
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

        final Match match = findMatchOrThrow(matchId);

        try {
            final boolean includeSeries =
                    inviteForm.isInviteSeries() && match.isRecurringOccurrence();
            matchParticipationService.inviteUser(
                    matchId, user, inviteForm.getEmail(), includeSeries);
            redirectAttributes.addFlashAttribute(
                    "hostAction", includeSeries ? "seriesInviteSent" : "inviteSent");
            return redirectToMatch(matchId);
        } catch (final MatchParticipationException e) {
            throwIfAccessFailure(e);
            final String errorMsg = inviteErrorMessage(e, inviteForm.getEmail(), locale);
            redirectAttributes.addFlashAttribute("hostActionTarget", "invites");
            redirectAttributes.addFlashAttribute("hostInviteEmail", inviteForm.getEmail());
            redirectAttributes.addFlashAttribute("hostActionError", errorMsg);
            return redirectToMatch(matchId);
        }
    }

    private String inviteErrorMessage(
            final MatchParticipationException exception, final String email, final Locale locale) {
        if (exception instanceof MatchParticipationUserNotFoundException) {
            return messageSource.getMessage(
                    "host.invites.error.userNotFound", new Object[] {email}, locale);
        }
        final String key =
                switch (exception) {
                    case MatchParticipationAlreadyJoinedException ignored ->
                            "host.invites.error.alreadyJoined";
                    case MatchParticipationAlreadyInvitedException ignored ->
                            "host.invites.error.alreadyInvited";
                    case MatchParticipationFullException ignored -> "host.invites.error.full";
                    case MatchParticipationIsHostException ignored -> "host.invites.error.isHost";
                    case MatchParticipationClosedException ignored -> "host.invites.error.closed";
                    case MatchParticipationSeriesStartedException ignored ->
                            "host.invites.error.seriesStarted";
                    case MatchParticipationSeriesClosedException ignored ->
                            "host.invites.error.seriesClosed";
                    case MatchParticipationSeriesAlreadyJoinedException ignored ->
                            "host.invites.error.seriesAlreadyJoined";
                    case MatchParticipationSeriesAlreadyInvitedException ignored ->
                            "host.invites.error.seriesAlreadyInvited";
                    case MatchParticipationSeriesAlreadyCoveredException ignored ->
                            "host.invites.error.seriesAlreadyCovered";
                    case MatchParticipationSeriesFullException ignored ->
                            "host.invites.error.seriesFull";
                    default -> "host.invites.error.generic";
                };
        return messageSource.getMessage(key, null, locale);
    }

    @PostMapping("/host/matches/{matchId:\\d+}/participants/{userId:\\d+}/remove")
    public ModelAndView removeParticipant(
            @AuthenticatedUser final User user,
            @PathVariable("matchId") final Long matchId,
            @PathVariable("userId") final Long userId,
            final Locale locale,
            final RedirectAttributes redirectAttributes) {
        final User targetUser = findUserOrThrow(userId);

        try {
            matchParticipationService.removeParticipant(matchId, user, targetUser);
            redirectAttributes.addFlashAttribute("hostAction", "participantRemoved");
            return redirectToMatch(matchId);
        } catch (final MatchParticipationException e) {
            throwIfAccessFailure(e);
            redirectAttributes.addFlashAttribute("hostActionTarget", "participants");
            redirectAttributes.addFlashAttribute(
                    "hostActionError", participantErrorMessage(e, locale));
            return redirectToMatch(matchId);
        }
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
        if (e instanceof MatchParticipationForbiddenException
                || e instanceof MatchParticipationNotFoundException) {
            throw participationAccessStatus(e);
        }
    }

    private ResponseStatusException participationAccessStatus(final MatchParticipationException e) {
        if (e instanceof MatchParticipationNotFoundException) {
            return new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (e instanceof MatchParticipationForbiddenException) {
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

    private String requestErrorMessage(
            final MatchParticipationException exception, final Locale locale) {
        final String key =
                switch (exception) {
                    case MatchParticipationFullException ignored ->
                            "event.host.requests.error.full";
                    case MatchParticipationClosedException ignored ->
                            "event.host.requests.error.closed";
                    default -> "event.host.requests.error.noPendingRequest";
                };
        return messageSource.getMessage(key, null, locale);
    }

    private String participantErrorMessage(
            final MatchParticipationException exception, final Locale locale) {
        final String key =
                switch (exception) {
                    case MatchParticipationStartedException ignored ->
                            "event.host.participants.error.started";
                    case MatchParticipationNotCancellableException ignored ->
                            "event.host.participants.error.notCancellable";
                    case MatchParticipationNotJoinedException ignored ->
                            "event.host.participants.error.notParticipant";
                    case MatchParticipationNotParticipantException ignored ->
                            "event.host.participants.error.notParticipant";
                    default -> "event.host.participants.error.generic";
                };
        return messageSource.getMessage(key, null, locale);
    }
}
