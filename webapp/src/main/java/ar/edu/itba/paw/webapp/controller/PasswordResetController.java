package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.services.AccountAuthService;
import ar.edu.itba.paw.services.PasswordResetPreview;
import ar.edu.itba.paw.services.VerificationConfirmationResult;
import ar.edu.itba.paw.services.exceptions.PasswordResetException;
import ar.edu.itba.paw.services.exceptions.VerificationFailureException;
import ar.edu.itba.paw.webapp.form.ResetPasswordForm;
import ar.edu.itba.paw.webapp.utils.VerificationViews;
import ar.edu.itba.paw.webapp.viewmodel.ShellViewModelFactory;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Objects;
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
        this.accountAuthService = Objects.requireNonNull(accountAuthService);
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @ModelAttribute("resetPasswordForm")
    public ResetPasswordForm resetPasswordForm() {
        return new ResetPasswordForm();
    }

    @GetMapping("/password-reset/{token}")
    public ModelAndView showPasswordReset(
            @PathVariable("token") final String token, final Locale locale) {
        try {
            return passwordResetView(
                    token,
                    accountAuthService.getPasswordResetPreview(token),
                    new ResetPasswordForm(),
                    locale);
        } catch (final VerificationFailureException exception) {
            return buildErrorView(exception, locale);
        }
    }

    @PostMapping("/password-reset/{token}")
    public ModelAndView resetPassword(
            @PathVariable("token") final String token,
            @Valid @ModelAttribute("resetPasswordForm") final ResetPasswordForm resetPasswordForm,
            final BindingResult bindingResult,
            final Locale locale) {
        if (!bindingResult.hasFieldErrors("password")
                && !bindingResult.hasFieldErrors("confirmPassword")
                && !resetPasswordForm
                        .getPassword()
                        .equals(resetPasswordForm.getConfirmPassword())) {
            bindingResult.rejectValue(
                    "confirmPassword",
                    "auth.validation.passwordMismatch",
                    messageSource.getMessage(
                            "auth.validation.passwordMismatch",
                            null,
                            Objects.requireNonNull(locale)));
        }

        if (bindingResult.hasErrors()) {
            try {
                return passwordResetView(
                        token,
                        accountAuthService.getPasswordResetPreview(token),
                        resetPasswordForm,
                        locale);
            } catch (final VerificationFailureException exception) {
                return buildErrorView(exception, locale);
            }
        }

        try {
            final VerificationConfirmationResult result =
                    accountAuthService.resetPassword(token, resetPasswordForm.getPassword());
            return new ModelAndView("redirect:" + result.getRedirectUrl());
        } catch (final PasswordResetException exception) {
            bindingResult.rejectValue(
                    "password",
                    Objects.requireNonNull(exception.getCode()),
                    Objects.requireNonNull(exception.getMessage()));
            try {
                return passwordResetView(
                        token,
                        accountAuthService.getPasswordResetPreview(token),
                        resetPasswordForm,
                        locale);
            } catch (final VerificationFailureException verificationFailureException) {
                return buildErrorView(verificationFailureException, locale);
            }
        } catch (final VerificationFailureException exception) {
            return buildErrorView(exception, locale);
        }
    }

    private ModelAndView passwordResetView(
            final String token,
            final PasswordResetPreview preview,
            final ResetPasswordForm resetPasswordForm,
            final Locale locale) {
        final ModelAndView mav = new ModelAndView("auth/reset-password");
        mav.addObject(
                "shell",
                ShellViewModelFactory.playerShell(messageSource, locale, "/password-reset"));
        mav.addObject("resetPath", "/password-reset/" + token);
        mav.addObject("resetPreview", preview);
        mav.addObject(
                "expiresAtLabel",
                VerificationViews.expiryFormatter(locale)
                        .format(preview.getExpiresAt().atZone(ZoneId.systemDefault())));
        mav.addObject("resetPasswordForm", resetPasswordForm);
        return mav;
    }

    private ModelAndView buildErrorView(
            final VerificationFailureException exception, final Locale locale) {
        return VerificationViews.buildErrorView(
                exception, messageSource, locale, "/forgot-password");
    }
}
