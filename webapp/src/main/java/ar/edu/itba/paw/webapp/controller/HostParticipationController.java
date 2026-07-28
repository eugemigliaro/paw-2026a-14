package ar.edu.itba.paw.webapp.controller;

import static ar.edu.itba.paw.webapp.utils.ImageUrlHelper.profileUrlFor;

import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.PendingJoinRequest;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.exceptions.match.MatchException;
import ar.edu.itba.paw.models.exceptions.matchParticipation.MatchParticipationException;
import ar.edu.itba.paw.services.MatchInvitationResult;
import ar.edu.itba.paw.services.MatchParticipationService;
import ar.edu.itba.paw.services.MatchService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.form.InviteForm;
import ar.edu.itba.paw.webapp.security.annotation.AuthenticatedUser;
import ar.edu.itba.paw.webapp.utils.PaginationUtils;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
public class HostParticipationController {

    private static final int REQUESTS_PAGE_SIZE = 10;

    private final MatchParticipationService matchParticipationService;
    private final MatchService matchService;
    private final UserService userService;

    @Autowired
    public HostParticipationController(
            final MatchParticipationService matchParticipationService,
            final MatchService matchService,
            final UserService userService) {
        this.matchParticipationService = matchParticipationService;
        this.matchService = matchService;
        this.userService = userService;
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
        matchParticipationService.findPendingRequests(matchId, user);
        return new ModelAndView("redirect:/matches/" + matchId + "#pending-requests");
    }

    @GetMapping("/host/requests")
    public ModelAndView showAllPendingRequests(
            @AuthenticatedUser final User user,
            @RequestParam(value = "page", defaultValue = "1") final int page,
            final Locale locale) {
        final PaginatedResult<PendingJoinRequest> result =
                matchParticipationService.findPendingRequestsForHost(
                        user, page, REQUESTS_PAGE_SIZE);

        final ModelAndView mav = new ModelAndView("host/participation/aggregate-requests");
        mav.addObject("aggregateRequests", true);
        mav.addObject("pendingRequests", result.getItems());
        mav.addObject(
                "requestActionsDisabledByMatchId",
                requestActionsDisabledByMatchId(result.getItems(), user));
        mav.addObject("matchesUrl", "/matches");
        mav.addObject("userProfileImageUrls", userProfileImageUrls(result.getItems()));
        mav.addObject("pageNumber", result.getPage());
        mav.addObject("totalPages", result.getTotalPages());
        mav.addObject("hasPreviousPage", result.hasPrevious());
        mav.addObject("hasNextPage", result.hasNext());
        mav.addObject("previousPageHref", buildRequestsPageUrl(result.getPage() - 1));
        mav.addObject("nextPageHref", buildRequestsPageUrl(result.getPage() + 1));
        mav.addObject(
                "paginationItems",
                PaginationUtils.buildPaginationItems(
                        result.getPage(), result.getTotalPages(), this::buildRequestsPageUrl));
        return mav;
    }

    private String buildRequestsPageUrl(final int page) {
        return UriComponentsBuilder.fromPath("/host/requests")
                .queryParam("page", page)
                .build()
                .encode()
                .toUriString();
    }

    private Map<Long, String> userProfileImageUrls(final List<PendingJoinRequest> joinRequests) {
        final Map<Long, String> urls = new LinkedHashMap<>();
        for (final PendingJoinRequest request : joinRequests) {
            final User user = request.getUser();
            if (user.getId() != null) {
                urls.put(user.getId(), profileUrlFor(user));
            }
        }
        return urls;
    }

    @PostMapping("/host/matches/{matchId:\\d+}/requests/{userId:\\d+}/approve")
    public ModelAndView approveRequest(
            @AuthenticatedUser final User user,
            @PathVariable("matchId") final Long matchId,
            @PathVariable("userId") final Long userId,
            final RedirectAttributes redirectAttributes) {
        final User targetUser = findUserOrThrow(userId);

        try {
            matchParticipationService.approveRequest(matchId, user, targetUser);
            redirectAttributes.addFlashAttribute("hostAction", HostAction.REQUEST_APPROVED);
        } catch (final MatchException e) {
            redirectAttributes.addFlashAttribute("hostActionTarget", HostActionTarget.REQUESTS);
            redirectAttributes.addFlashAttribute(
                    "hostActionErrorCode", "event.host.requests.error." + e.getMessage());
        }
        return redirectToMatch(matchId);
    }

    @PostMapping("/host/matches/{matchId:\\d+}/requests/{userId:\\d+}/reject")
    public ModelAndView rejectRequest(
            @AuthenticatedUser final User user,
            @PathVariable("matchId") final Long matchId,
            @PathVariable("userId") final Long userId,
            final RedirectAttributes redirectAttributes) {
        final User targetUser = findUserOrThrow(userId);

        try {
            matchParticipationService.rejectRequest(matchId, user, targetUser);
            redirectAttributes.addFlashAttribute("hostAction", HostAction.REQUEST_REJECTED);
        } catch (final MatchParticipationException e) {
            redirectAttributes.addFlashAttribute("hostActionTarget", HostActionTarget.REQUESTS);
            redirectAttributes.addFlashAttribute(
                    "hostActionErrorCode", "event.host.requests.error." + e.getMessage());
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
            final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("hostActionTarget", HostActionTarget.INVITES);
            redirectAttributes.addFlashAttribute("hostInviteEmail", inviteForm.getEmail());
            redirectAttributes.addFlashAttribute(
                    "hostActionErrorCode", inviteValidationErrorCode(bindingResult));
            return redirectToMatch(matchId);
        }

        try {
            final MatchInvitationResult invitationResult =
                    matchParticipationService.inviteUserWithResult(
                            matchId, user, inviteForm.getEmail(), inviteForm.isInviteSeries());
            redirectAttributes.addFlashAttribute(
                    "hostAction",
                    invitationResult.isSeriesInvitation()
                            ? HostAction.SERIES_INVITE_SENT
                            : HostAction.INVITE_SENT);
            return redirectToMatch(matchId);
        } catch (final MatchException e) {
            redirectAttributes.addFlashAttribute("hostActionTarget", HostActionTarget.INVITES);
            redirectAttributes.addFlashAttribute("hostInviteEmail", inviteForm.getEmail());
            redirectAttributes.addFlashAttribute(
                    "hostActionErrorCode", "host.invites.error." + e.getMessage());
        }
        return redirectToMatch(matchId);
    }

    @PostMapping("/host/matches/{matchId:\\d+}/participants/{userId:\\d+}/remove")
    public ModelAndView removeParticipant(
            @AuthenticatedUser final User user,
            @PathVariable("matchId") final Long matchId,
            @PathVariable("userId") final Long userId,
            final RedirectAttributes redirectAttributes) {
        final User targetUser = findUserOrThrow(userId);

        try {
            matchParticipationService.removeParticipant(matchId, user, targetUser);
            redirectAttributes.addFlashAttribute("hostAction", HostAction.PARTICIPANT_REMOVED);
        } catch (final MatchException e) {
            redirectAttributes.addFlashAttribute("hostActionTarget", HostActionTarget.PARTICIPANTS);
            redirectAttributes.addFlashAttribute(
                    "hostActionErrorCode", "event.host.participants.error." + e.getMessage());
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

    private Map<Long, Boolean> requestActionsDisabledByMatchId(
            final List<PendingJoinRequest> pendingRequests, final User user) {
        final Map<Long, Boolean> disabledByMatchId = new HashMap<>();
        for (final PendingJoinRequest request : pendingRequests) {
            if (request.getMatch() == null || request.getMatch().getId() == null) {
                continue;
            }
            final Long matchId = request.getMatch().getId();
            if (disabledByMatchId.containsKey(matchId)) {
                continue;
            }

            final boolean disabled =
                    !matchService
                            .actionCapabilities(request.getMatch(), user)
                            .isCanManageParticipants();
            disabledByMatchId.put(matchId, disabled);
        }
        return disabledByMatchId;
    }

    private String inviteValidationErrorCode(final BindingResult bindingResult) {
        return bindingResult.getAllErrors().stream()
                .findFirst()
                .map(ObjectError::getDefaultMessage)
                .map(this::stripBraces)
                .orElse("host.invites.error.generic");
    }

    private String stripBraces(final String message) {
        if (message == null || message.isBlank()) {
            return "host.invites.error.generic";
        }

        return message.length() >= 2
                        && message.charAt(0) == '{'
                        && message.charAt(message.length() - 1) == '}'
                ? message.substring(1, message.length() - 1)
                : message;
    }
}
