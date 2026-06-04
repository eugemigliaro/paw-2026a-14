package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.ModerationReport;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserBan;
import ar.edu.itba.paw.services.ModerationService;
import ar.edu.itba.paw.services.exceptions.moderation.ModerationAppealLimitException;
import ar.edu.itba.paw.services.exceptions.moderation.ModerationAppealRejectedException;
import ar.edu.itba.paw.services.exceptions.moderation.ModerationReportNotFoundException;
import ar.edu.itba.paw.webapp.utils.SecurityControllerUtils;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/account/ban")
public class UserBanAppealController {

    private final ModerationService moderationService;
    private final MessageSource messageSource;

    public UserBanAppealController(
            final ModerationService moderationService, final MessageSource messageSource) {
        this.moderationService = moderationService;
        this.messageSource = messageSource;
    }

    @GetMapping
    public ModelAndView showBanPage(final Model model, final Locale locale) {
        final User user =
                SecurityControllerUtils.requireAuthenticatedUser(); // TODO: add controller advice
        final UserBan activeBan =
                moderationService
                        .findActiveBan(user)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        final ModerationReport report =
                moderationService
                        .findReportById(activeBan.getModerationReport().getId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        final ModelAndView mav = new ModelAndView("account/banned");
        mav.addObject(
                "pageTitle", messageSource.getMessage("page.title.accountBanned", null, locale));
        mav.addObject("banTitle", messageSource.getMessage("account.ban.title", null, locale));
        mav.addObject(
                "banDescription",
                messageSource.getMessage(
                        report.getAppealCount() > 0
                                ? "account.ban.description"
                                : "account.ban.description.appeal.available",
                        null,
                        locale));
        mav.addObject(
                "banUntilLabel",
                DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                        .withLocale(locale)
                        .withZone(ZoneId.systemDefault())
                        .format(activeBan.getBannedUntil()));
        mav.addObject(
                "banReason",
                messageSource.getMessage(
                        "moderation.reason." + report.getReason().getDbValue(), null, locale));
        mav.addObject("appealReason", report.getAppealReason());
        mav.addObject("appealAllowed", report.getAppealCount() < 1);
        mav.addObject("action", model.asMap().get("action"));
        return mav;
    }

    @PostMapping("/appeal")
    public ModelAndView appealBan(
            @RequestParam("appealReason") final String appealReason,
            final RedirectAttributes redirectAttributes,
            final Locale locale) {
        final User user =
                SecurityControllerUtils.requireAuthenticatedUser(); // TODO: add controller advice
        final UserBan activeBan =
                moderationService
                        .findActiveBan(user)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        String exceptionReason;
        try {
            moderationService.appealReport(activeBan.getModerationReport().getId(), appealReason);
            redirectAttributes.addFlashAttribute("action", "appealed");
            return new ModelAndView("redirect:/account/ban");
        } catch (
                final ModerationReportNotFoundException
                        exception) { // TODO: move message code to service (?) and catch generic
            // exception here
            exceptionReason = "report_not_found";
        } catch (final ModerationAppealLimitException exception) {
            exceptionReason = "appeal_limit";
        } catch (final ModerationAppealRejectedException exception) {
            exceptionReason = "appeal_rejected";
        }
        return new ModelAndView("redirect:/account/ban?error=" + exceptionReason);
    }
}
