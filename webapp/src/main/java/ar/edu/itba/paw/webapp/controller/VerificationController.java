package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.services.AccountAuthService;
import ar.edu.itba.paw.services.VerificationConfirmationResult;
import ar.edu.itba.paw.services.VerificationFailureException;
import ar.edu.itba.paw.services.VerificationFailureReason;
import ar.edu.itba.paw.services.VerificationPreview;
import ar.edu.itba.paw.webapp.viewmodel.ShellViewModelFactory;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class VerificationController {

    private final AccountAuthService accountAuthService;
    private final MessageSource messageSource;

    @Autowired
    public VerificationController(
            final AccountAuthService accountAuthService, final MessageSource messageSource) {
        this.accountAuthService = accountAuthService;
        this.messageSource = messageSource;
    }

    @GetMapping("/verifications/{token}")
    public ModelAndView showVerification(
            @PathVariable("token") final String token, final Locale locale) {
        try {
            final VerificationPreview preview = accountAuthService.getVerificationPreview(token);
            final ModelAndView mav = new ModelAndView("verification/confirm");
            mav.addObject("shell", ShellViewModelFactory.browseShell(messageSource, locale));
            mav.addObject("preview", preview);
            mav.addObject("confirmPath", "/verifications/" + token + "/confirm");
            mav.addObject(
                    "expiresAtLabel",
                    expiryFormatter(locale)
                            .format(preview.getExpiresAt().atZone(ZoneId.systemDefault())));
            return mav;
        } catch (final VerificationFailureException exception) {
            return buildErrorView(exception, locale);
        }
    }

    @PostMapping("/verifications/{token}/confirm")
    public ModelAndView confirm(@PathVariable("token") final String token, final Locale locale) {
        try {
            final VerificationConfirmationResult result =
                    accountAuthService.confirmVerification(token);
            return new ModelAndView("redirect:" + result.getRedirectUrl());
        } catch (final VerificationFailureException exception) {
            return buildErrorView(exception, locale);
        }
    }

    private ModelAndView buildErrorView(
            final VerificationFailureException exception, final Locale locale) {
        final ModelAndView mav = new ModelAndView("verification/error");
        mav.addObject("shell", ShellViewModelFactory.browseShell(messageSource, locale));
        mav.addObject("title", titleFor(exception.getReason(), locale));
        mav.addObject("message", exception.getMessage());
        mav.addObject("backHref", "/");
        return mav;
    }

    private String titleFor(final VerificationFailureReason reason, final Locale locale) {
        switch (reason) {
            case EXPIRED:
                return messageSource.getMessage("verification.error.expired", null, locale);
            case ALREADY_USED:
                return messageSource.getMessage("verification.error.alreadyUsed", null, locale);
            case INVALID_ACTION:
                return messageSource.getMessage("verification.error.invalidAction", null, locale);
            case NOT_FOUND:
            default:
                return messageSource.getMessage("verification.error.notFound", null, locale);
        }
    }

    private static DateTimeFormatter expiryFormatter(final Locale locale) {
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                .withLocale(locale == null ? Locale.ENGLISH : locale);
    }
}
