package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.services.AccountAuthService;
import ar.edu.itba.paw.services.VerificationConfirmationResult;
import ar.edu.itba.paw.services.VerificationPreview;
import ar.edu.itba.paw.services.exceptions.verificationFailure.VerificationFailureAlreadyUsedException;
import ar.edu.itba.paw.services.exceptions.verificationFailure.VerificationFailureException;
import ar.edu.itba.paw.services.exceptions.verificationFailure.VerificationFailureExpiredException;
import ar.edu.itba.paw.services.exceptions.verificationFailure.VerificationFailureInvalidActionException;
import ar.edu.itba.paw.services.exceptions.verificationFailure.VerificationFailureNotFoundException;
import ar.edu.itba.paw.webapp.utils.SecurityControllerUtils;
import ar.edu.itba.paw.webapp.utils.VerificationViews;
import java.time.ZoneId;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
        String messageKey;
        try {
            final VerificationPreview preview = accountAuthService.getVerificationPreview(token);
            final ModelAndView mav = new ModelAndView("verification/confirm");
            mav.addObject("preview", preview);
            mav.addObject("confirmPath", "/verifications/" + token + "/confirm");
            mav.addObject(
                    "expiresAtLabel",
                    VerificationViews.expiryFormatter(locale)
                            .format(preview.getExpiresAt().atZone(ZoneId.systemDefault())));
            return mav;
        } catch (
                final VerificationFailureNotFoundException
                        exception) { // TODO: move message code to service (?) and catch generic
            // exception here
            messageKey = "verification.message.notFound";
        } catch (final VerificationFailureAlreadyUsedException exception) {
            messageKey = "verification.message.alreadyUsed";
        } catch (final VerificationFailureExpiredException exception) {
            messageKey = "verification.message.expired";
        } catch (final VerificationFailureInvalidActionException exception) {
            messageKey = "verification.message.accountUnavailable";
        } catch (final VerificationFailureException exception) {
            messageKey = "verification.message.accountUnavailable";
        }

        return VerificationViews.buildErrorView(messageKey, messageSource, locale, "/");
    }

    @PostMapping("/verifications/{token}/confirm")
    public ModelAndView confirm(
            @PathVariable("token") final String token,
            final Locale locale,
            final HttpServletRequest request,
            final HttpServletResponse response) {
        String messageKey;

        try {
            final VerificationConfirmationResult result =
                    accountAuthService.confirmVerification(token);
            result.getAccount()
                    .ifPresent(
                            account ->
                                    SecurityControllerUtils.authenticateVerifiedAccount(
                                            account, request, response));
            return new ModelAndView("redirect:/");
        } catch (
                final VerificationFailureNotFoundException
                        exception) { // TODO: move message code to service (?) and catch generic
            // exception here
            messageKey = "verification.message.notFound";
        } catch (final VerificationFailureAlreadyUsedException exception) {
            messageKey = "verification.message.alreadyUsed";
        } catch (final VerificationFailureExpiredException exception) {
            messageKey = "verification.message.expired";
        } catch (final VerificationFailureInvalidActionException exception) {
            messageKey = "verification.message.accountUnavailable";
        } catch (final VerificationFailureException exception) {
            messageKey = "verification.message.accountUnavailable";
        }

        return VerificationViews.buildErrorView(messageKey, messageSource, locale, "/");
    }
}
