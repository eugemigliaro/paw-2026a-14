package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.ModerationReport;
import ar.edu.itba.paw.models.UserBan;
import ar.edu.itba.paw.services.ModerationService;
import ar.edu.itba.paw.services.exceptions.ModerationException;
import ar.edu.itba.paw.webapp.security.AuthenticatedUserPrincipal;
import ar.edu.itba.paw.webapp.security.CurrentAuthenticatedUser;
import ar.edu.itba.paw.webapp.viewmodel.ShellViewModelFactory;
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
        final long userId = currentUserId();
        final UserBan activeBan =
                moderationService
                        .findActiveBan(userId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        final ModerationReport report =
                moderationService
                        .findReportById(activeBan.getModerationReportId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        final ModelAndView mav = new ModelAndView("account/banned");
        mav.addObject(
                "shell", ShellViewModelFactory.playerShell(messageSource, locale, "/account/ban"));
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
        mav.addObject("banReason", report.getReason());
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
        final long userId = currentUserId();
        final UserBan activeBan =
                moderationService
                        .findActiveBan(userId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        try {
            moderationService.appealReport(activeBan.getModerationReportId(), appealReason);
            redirectAttributes.addFlashAttribute("action", "appealed");
            return new ModelAndView("redirect:/account/ban");
        } catch (final ModerationException exception) {
            return new ModelAndView("redirect:/account/ban?error=" + exception.getCode());
        }
    }

    private static long currentUserId() {
        return CurrentAuthenticatedUser.get()
                .map(AuthenticatedUserPrincipal::getUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
