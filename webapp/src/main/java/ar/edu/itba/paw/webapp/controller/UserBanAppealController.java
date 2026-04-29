package ar.edu.itba.paw.webapp.controller;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

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
    public ModelAndView showBanPage(final Locale locale) {
        final long userId = currentUserId();
        final UserBan activeBan =
                moderationService
                        .findActiveBan(userId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        final ModelAndView mav = new ModelAndView("account/banned");
        mav.addObject(
                "shell", ShellViewModelFactory.playerShell(messageSource, locale, "/account/ban"));
        mav.addObject(
                "pageTitle", messageSource.getMessage("page.title.accountBanned", null, locale));
        mav.addObject("banTitle", messageSource.getMessage("account.ban.title", null, locale));
        mav.addObject(
                "banDescription",
                messageSource.getMessage("account.ban.description", null, locale));
        mav.addObject(
                "banUntilLabel",
                DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                        .withLocale(locale)
                        .withZone(ZoneId.systemDefault())
                        .format(activeBan.getBannedUntil()));
        mav.addObject("banReason", activeBan.getReason());
        mav.addObject("appealReason", activeBan.getAppealReason());
        mav.addObject("appealAllowed", activeBan.getAppealCount() < 1);
        return mav;
    }

    @PostMapping("/appeal")
    public ModelAndView appealBan(
            @RequestParam("appealReason") final String appealReason, final Locale locale) {
        final long userId = currentUserId();
        final UserBan activeBan =
                moderationService
                        .findActiveBan(userId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        try {
            moderationService.appealBan(activeBan.getId(), userId, appealReason);
            return new ModelAndView("redirect:/account/ban?action=appealed");
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
