package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.ReportReason;
import ar.edu.itba.paw.models.ReportTargetType;
import ar.edu.itba.paw.services.ModerationService;
import ar.edu.itba.paw.services.exceptions.ModerationException;
import ar.edu.itba.paw.webapp.security.AuthenticatedUserPrincipal;
import ar.edu.itba.paw.webapp.security.CurrentAuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ModerationReportController {

    private final ModerationService moderationService;

    public ModerationReportController(final ModerationService moderationService) {
        this.moderationService = moderationService;
    }

    @PostMapping("/reports/matches/{matchId}")
    public ModelAndView reportMatch(
            @PathVariable("matchId") final Long matchId,
            @RequestParam("reason") final String reason,
            @RequestParam(value = "details", required = false) final String details) {
        final AuthenticatedUserPrincipal principal = requireAuthenticatedUser();
        final ReportReason parsedReason = parseReason(reason);
        try {
            moderationService.reportContent(
                    principal.getUserId(), ReportTargetType.MATCH, matchId, parsedReason, details);
            return new ModelAndView("redirect:/matches/" + matchId + "?report=sent#moderation");
        } catch (final ModerationException exception) {
            return new ModelAndView(
                    "redirect:/matches/"
                            + matchId
                            + "?reportError="
                            + exception.getCode()
                            + "#moderation");
        }
    }

    @PostMapping("/reports/reviews/{reviewId}")
    public ModelAndView reportReview(
            @PathVariable("reviewId") final Long reviewId,
            @RequestParam("username") final String username,
            @RequestParam("reason") final String reason,
            @RequestParam(value = "details", required = false) final String details) {
        final AuthenticatedUserPrincipal principal = requireAuthenticatedUser();
        final ReportReason parsedReason = parseReason(reason);
        try {
            moderationService.reportContent(
                    principal.getUserId(),
                    ReportTargetType.REVIEW,
                    reviewId,
                    parsedReason,
                    details);
            return new ModelAndView("redirect:/users/" + username + "?report=sent#reviews");
        } catch (final ModerationException exception) {
            return new ModelAndView(
                    "redirect:/users/"
                            + username
                            + "?reportError="
                            + exception.getCode()
                            + "#reviews");
        }
    }

    private static ReportReason parseReason(final String reason) {
        return ReportReason.fromDbValue(reason)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));
    }

    private static AuthenticatedUserPrincipal requireAuthenticatedUser() {
        return CurrentAuthenticatedUser.get()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
