package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.services.ActionVerificationService;
import ar.edu.itba.paw.services.VerificationConfirmationResult;
import ar.edu.itba.paw.services.VerificationFailureException;
import ar.edu.itba.paw.services.VerificationFailureReason;
import ar.edu.itba.paw.services.VerificationPreview;
import ar.edu.itba.paw.webapp.viewmodel.PawUiMockData;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class VerificationController {

    private static final DateTimeFormatter EXPIRY_FORMATTER =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                    .withLocale(Locale.US);

    private final ActionVerificationService actionVerificationService;

    @Autowired
    public VerificationController(final ActionVerificationService actionVerificationService) {
        this.actionVerificationService = actionVerificationService;
    }

    @GetMapping("/verifications/{token}")
    public ModelAndView showVerification(@PathVariable("token") final String token) {
        try {
            final VerificationPreview preview = actionVerificationService.getPreview(token);
            final ModelAndView mav = new ModelAndView("verification/confirm");
            mav.addObject("shell", PawUiMockData.browseShell());
            mav.addObject("preview", preview);
            mav.addObject("confirmPath", "/verifications/" + token + "/confirm");
            mav.addObject(
                    "expiresAtLabel",
                    EXPIRY_FORMATTER.format(preview.getExpiresAt().atZone(ZoneId.systemDefault())));
            return mav;
        } catch (final VerificationFailureException exception) {
            return buildErrorView(exception);
        }
    }

    @PostMapping("/verifications/{token}/confirm")
    public ModelAndView confirm(@PathVariable("token") final String token) {
        try {
            final VerificationConfirmationResult result = actionVerificationService.confirm(token);
            return new ModelAndView("redirect:" + result.getRedirectUrl());
        } catch (final VerificationFailureException exception) {
            return buildErrorView(exception);
        }
    }

    private ModelAndView buildErrorView(final VerificationFailureException exception) {
        final ModelAndView mav = new ModelAndView("verification/error");
        mav.addObject("shell", PawUiMockData.browseShell());
        mav.addObject("title", titleFor(exception.getReason()));
        mav.addObject("message", exception.getMessage());
        mav.addObject("backHref", "/");
        return mav;
    }

    private static String titleFor(final VerificationFailureReason reason) {
        switch (reason) {
            case EXPIRED:
                return "This verification link expired";
            case ALREADY_USED:
                return "This verification link was already used";
            case INVALID_ACTION:
                return "This action can no longer be completed";
            case NOT_FOUND:
            default:
                return "We could not find that verification link";
        }
    }
}
