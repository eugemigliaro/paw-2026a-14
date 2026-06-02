package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.MatchParticipationService;
import ar.edu.itba.paw.services.exceptions.MatchParticipationException;
import ar.edu.itba.paw.webapp.utils.SecurityControllerUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PlayerParticipationController {

    private final MatchParticipationService matchParticipationService;

    @Autowired
    public PlayerParticipationController(
            final MatchParticipationService matchParticipationService) {
        this.matchParticipationService = matchParticipationService;
    }

    @PostMapping("/matches/{matchId}/join-requests")
    public ModelAndView requestToJoin(
            @PathVariable("matchId") final Long matchId,
            final RedirectAttributes redirectAttributes) {
        final User user = SecurityControllerUtils.requireAuthenticatedUser();

        try {
            matchParticipationService.requestToJoin(matchId, user);
            redirectAttributes.addFlashAttribute("joinRequested", true);
            return new ModelAndView("redirect:/matches/" + matchId);
        } catch (final MatchParticipationException e) {
            return new ModelAndView("redirect:/matches/" + matchId + "?joinError=" + e.getCode());
        }
    }

    @PostMapping({
        "/matches/{matchId}/recurring-join-requests",
        "/matches/{matchId}/series-join-requests"
    })
    public ModelAndView requestToJoinSeries(
            @PathVariable("matchId") final Long matchId,
            final RedirectAttributes redirectAttributes) {
        final User user = SecurityControllerUtils.requireAuthenticatedUser();

        try {
            matchParticipationService.requestToJoinSeries(matchId, user);
            redirectAttributes.addFlashAttribute("seriesJoinRequested", true);
            return new ModelAndView("redirect:/matches/" + matchId);
        } catch (final MatchParticipationException e) {
            return new ModelAndView("redirect:/matches/" + matchId + "?joinError=" + e.getCode());
        }
    }

    @PostMapping("/matches/{matchId}/join-requests/cancel")
    public ModelAndView cancelJoinRequest(
            @PathVariable("matchId") final Long matchId,
            final RedirectAttributes redirectAttributes) {
        final User user = SecurityControllerUtils.requireAuthenticatedUser();

        try {
            matchParticipationService.cancelJoinRequest(matchId, user);
            redirectAttributes.addFlashAttribute("joinStatus", "cancelled");
            return new ModelAndView("redirect:/matches/" + matchId);
        } catch (final MatchParticipationException e) {
            return new ModelAndView("redirect:/matches/" + matchId + "?joinError=" + e.getCode());
        }
    }

    @PostMapping("/matches/{matchId}/invites/accept")
    public ModelAndView acceptInvite(
            @PathVariable("matchId") final Long matchId,
            final RedirectAttributes redirectAttributes) {
        final User user = SecurityControllerUtils.requireAuthenticatedUser();

        try {
            matchParticipationService.acceptInvite(matchId, user);
            redirectAttributes.addFlashAttribute("inviteStatus", "accepted");
            return new ModelAndView("redirect:/matches/" + matchId);
        } catch (final MatchParticipationException e) {
            return new ModelAndView("redirect:/matches/" + matchId + "?inviteError=" + e.getCode());
        }
    }

    @PostMapping("/matches/{matchId}/invites/decline")
    public ModelAndView declineInvite(@PathVariable("matchId") final Long matchId) {
        final User user = SecurityControllerUtils.requireAuthenticatedUser();

        try {
            matchParticipationService.declineInvite(matchId, user);
            return new ModelAndView("redirect:/events");
        } catch (final MatchParticipationException e) {
            return new ModelAndView("redirect:/matches/" + matchId + "?inviteError=" + e.getCode());
        }
    }
}
