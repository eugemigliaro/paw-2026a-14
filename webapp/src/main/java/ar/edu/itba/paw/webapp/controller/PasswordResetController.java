package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.services.AccountAuthService;
import ar.edu.itba.paw.services.PasswordResetPreview;
import ar.edu.itba.paw.services.exceptions.passwordReset.PasswordResetInvalidException;
import ar.edu.itba.paw.services.exceptions.verificationFailure.VerificationFailureAlreadyUsedException;
import ar.edu.itba.paw.services.exceptions.verificationFailure.VerificationFailureExpiredException;
import ar.edu.itba.paw.services.exceptions.verificationFailure.VerificationFailureInvalidActionException;
import ar.edu.itba.paw.services.exceptions.verificationFailure.VerificationFailureNotFoundException;
import ar.edu.itba.paw.webapp.form.ResetPasswordForm;
import ar.edu.itba.paw.webapp.utils.VerificationViews;
import java.time.ZoneId;
import java.util.Locale;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class PasswordResetController {

    private final AccountAuthService accountAuthService;
    private final MessageSource messageSource;

    @Autowired
    public PasswordResetController(
            final AccountAuthService accountAuthService, final MessageSource messageSource) {
        this.accountAuthService = accountAuthService;
        this.messageSource = messageSource;
    }

    @ModelAttribute("resetPasswordForm")
    public ResetPasswordForm resetPasswordForm() {
        return new ResetPasswordForm();
    }

    @GetMapping("/password-reset/{token}")
    public ModelAndView showPasswordReset(
            @PathVariable("token") final String token, final Locale locale) {
        String messageKey;
        try {
            return passwordResetView(
                    token,
                    accountAuthService.getPasswordResetPreview(token),
                    new ResetPasswordForm(),
                    locale);
        } catch (final VerificationFailureNotFoundException exception) {
            // TODO: move message code to service exception (?). Catch generic type and assign
            // e.getMessage().
            messageKey = "verification.message.notFound";
        } catch (final VerificationFailureAlreadyUsedException exception) {
            messageKey = "verification.message.alreadyUsed";
        } catch (final VerificationFailureExpiredException exception) {
            messageKey = "verification.message.expired";
        } catch (final VerificationFailureInvalidActionException exception) {
            messageKey = "passwordReset.message.unavailable";
        }
        return buildErrorView(messageKey, locale);
    }

    @PostMapping("/password-reset/{token}")
    public ModelAndView resetPassword(
            @PathVariable("token") final String token,
            @Valid @ModelAttribute("resetPasswordForm") final ResetPasswordForm resetPasswordForm,
            final BindingResult bindingResult,
            final Locale locale) {
        String messageKey;

        if (bindingResult.hasErrors()) {
            try {
                return passwordResetView(
                        token,
                        accountAuthService.getPasswordResetPreview(token),
                        resetPasswordForm,
                        locale);
            } catch (
                    final VerificationFailureNotFoundException
                            exception) { // TODO: same as showPasswordReset
                messageKey = "verification.message.notFound";
            } catch (final VerificationFailureAlreadyUsedException exception) {
                messageKey = "verification.message.alreadyUsed";
            } catch (final VerificationFailureExpiredException exception) {
                messageKey = "verification.message.expired";
            } catch (final VerificationFailureInvalidActionException exception) {
                messageKey = "passwordReset.message.unavailable";
            }
            return buildErrorView(messageKey, locale);
        }

        try {
            accountAuthService.resetPassword(token, resetPasswordForm.getPassword());
            return new ModelAndView("redirect:/login?reset=1");
        } catch (final PasswordResetInvalidException exception) {
            bindingResult.rejectValue(
                    "password", "auth.registration.error.passwordInvalid", exception.getMessage());
            try {
                return passwordResetView(
                        token,
                        accountAuthService.getPasswordResetPreview(token),
                        resetPasswordForm,
                        locale);
            } catch (
                    final VerificationFailureNotFoundException
                            e) { // TODO: same as showPasswordReset
                messageKey = "verification.message.notFound";
            } catch (final VerificationFailureAlreadyUsedException e) {
                messageKey = "verification.message.alreadyUsed";
            } catch (final VerificationFailureExpiredException e) {
                messageKey = "verification.message.expired";
            } catch (final VerificationFailureInvalidActionException e) {
                messageKey = "passwordReset.message.unavailable";
            }
        } catch (final VerificationFailureNotFoundException exception) {
            messageKey = "verification.message.notFound";
        } catch (final VerificationFailureAlreadyUsedException exception) {
            messageKey = "verification.message.alreadyUsed";
        } catch (final VerificationFailureExpiredException exception) {
            messageKey = "verification.message.expired";
        } catch (final VerificationFailureInvalidActionException exception) {
            messageKey = "passwordReset.message.unavailable";
        }
        return buildErrorView(messageKey, locale);
    }

    private ModelAndView passwordResetView(
            final String token,
            final PasswordResetPreview preview,
            final ResetPasswordForm resetPasswordForm,
            final Locale locale) {
        final ModelAndView mav = new ModelAndView("auth/reset-password");
        mav.addObject("resetPath", "/password-reset/" + token);
        mav.addObject("resetPreview", preview);
        mav.addObject(
                "expiresAtLabel",
                VerificationViews.expiryFormatter(locale)
                        .format(preview.getExpiresAt().atZone(ZoneId.systemDefault())));
        mav.addObject("resetPasswordForm", resetPasswordForm);
        return mav;
    }

    private ModelAndView buildErrorView(final String messageKey, final Locale locale) {
        return VerificationViews.buildErrorView(
                messageKey, messageSource, locale, "/forgot-password");
    }
}
