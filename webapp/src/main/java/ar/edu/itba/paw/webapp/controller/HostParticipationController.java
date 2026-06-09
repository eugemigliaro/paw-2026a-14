package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.PendingJoinRequest;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.exceptions.match.MatchException;
import ar.edu.itba.paw.models.exceptions.matchParticipation.MatchParticipationException;
import ar.edu.itba.paw.services.MatchInvitationResult;
import ar.edu.itba.paw.services.MatchParticipationService;
import ar.edu.itba.paw.services.UserService;
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

    private final MatchParticipationService matchParticipationService;
    private final UserService userService;
    private final MessageSource messageSource;

    @Autowired
    public HostParticipationController(
            final MatchParticipationService matchParticipationService,
            final UserService userService,
            final MessageSource messageSource) {
        this.matchParticipationService = matchParticipationService;
        this.userService = userService;
        this.messageSource = messageSource;
    }

    @GetMapping("/host/matches/{matchId:\\d+}/participants")
    public ModelAndView showRoster(
            @AuthenticatedUser final User user,
            @PathVariable("matchId") final Long matchId,
            final Locale locale) {
        matchParticipationService.findConfirmedParticipants(matchId, user);
        return new ModelAndView("redirect:/matches/" + matchId + "#participants");
    }

    @GetMapping("/host/matches/{matchId:\\d+}/requests")
    public ModelAndView showPendingRequests(
            @AuthenticatedUser final User user,
            @PathVariable("matchId") final Long matchId,
            final Locale locale) {
        try {
            matchParticipationService.findPendingRequests(matchId, user);
        } catch (final MatchException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
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
        mav.addObject("matchesUrl", "/matches");
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
        } catch (final MatchException e) {
            final String errorKey = "event.host.requests.error." + e.getMessage();
            final String errorMsg = messageSource.getMessage(errorKey, null, locale);
            redirectAttributes.addFlashAttribute("hostActionTarget", "requests");
            redirectAttributes.addFlashAttribute("hostActionError", errorMsg);
        }
        return redirectToMatch(matchId);
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
        } catch (final MatchParticipationException e) {
            final String errorKey = "event.host.requests.error." + e.getMessage();
            final String errorMsg = messageSource.getMessage(errorKey, null, locale);
            redirectAttributes.addFlashAttribute("hostActionTarget", "requests");
            redirectAttributes.addFlashAttribute("hostActionError", errorMsg);
        }
        return redirectToMatch(matchId);
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
        matchParticipationService.findInvitedUsers(matchId, user);
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

        try {
            final MatchInvitationResult invitationResult =
                    matchParticipationService.inviteUserWithResult(
                            matchId, user, inviteForm.getEmail(), inviteForm.isInviteSeries());
            redirectAttributes.addFlashAttribute(
                    "hostAction",
                    invitationResult.isSeriesInvitation() ? "seriesInviteSent" : "inviteSent");
            return redirectToMatch(matchId);
        } catch (final MatchException e) {
            final String errorKey = "host.invites.error." + e.getMessage();
            final String errorMsg = messageSource.getMessage(errorKey, null, locale);
            redirectAttributes.addFlashAttribute("hostActionTarget", "invites");
            redirectAttributes.addFlashAttribute("hostInviteEmail", inviteForm.getEmail());
            redirectAttributes.addFlashAttribute("hostActionError", errorMsg);
        }
        return redirectToMatch(matchId);
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
        } catch (final MatchException e) {
            final String errorKey = "event.host.participants.error." + e.getMessage();
            final String errorMsg = messageSource.getMessage(errorKey, null, locale);
            redirectAttributes.addFlashAttribute("hostActionTarget", "participants");
            redirectAttributes.addFlashAttribute("hostActionError", errorMsg);
        }
        return redirectToMatch(matchId);
    }

    private User findUserOrThrow(final Long userId) {
        return userService
                .findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private ModelAndView redirectToMatch(final Long matchId) {
        return new ModelAndView("redirect:/matches/" + matchId);
    }

    private String inviteValidationErrorMessage(
            final BindingResult bindingResult, final Locale locale) {
        return bindingResult.getAllErrors().stream()
                .findFirst()
                .map(error -> messageSource.getMessage(error, locale))
                .orElseGet(
                        () -> messageSource.getMessage("host.invites.error.generic", null, locale));
    }
}
