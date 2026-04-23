package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.services.AccountAuthService;
import ar.edu.itba.paw.services.RegisterAccountRequest;
import ar.edu.itba.paw.services.VerificationRequestResult;
import ar.edu.itba.paw.services.exceptions.AccountRegistrationException;
import ar.edu.itba.paw.webapp.form.ForgotPasswordForm;
import ar.edu.itba.paw.webapp.form.RegisterForm;
import ar.edu.itba.paw.webapp.security.CurrentAuthenticatedUser;
import ar.edu.itba.paw.webapp.utils.VerificationViews;
import ar.edu.itba.paw.webapp.viewmodel.ShellViewModelFactory;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Optional;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class AuthController {

    private final AccountAuthService accountAuthService;
    private final MessageSource messageSource;

    @Autowired
    public AuthController(
            final AccountAuthService accountAuthService, final MessageSource messageSource) {
        this.accountAuthService = accountAuthService;
        this.messageSource = messageSource;
    }

    @ModelAttribute("registerForm")
    public RegisterForm registerForm() {
        return new RegisterForm();
    }

    @ModelAttribute("forgotPasswordForm")
    public ForgotPasswordForm forgotPasswordForm() {
        return new ForgotPasswordForm();
    }

    @GetMapping("/login")
    public ModelAndView showLogin(
            @RequestParam(value = "error", required = false) final String error,
            @RequestParam(value = "email", required = false) final String email,
            @RequestParam(value = "verified", required = false) final String verified,
            @RequestParam(value = "reset", required = false) final String reset,
            @RequestParam(value = "logout", required = false) final String logout,
            @RequestParam(value = "continue", required = false) final String continueFlag,
            final Locale locale) {
        if (CurrentAuthenticatedUser.get().isPresent()) {
            return new ModelAndView("redirect:/");
        }

        final ModelAndView mav = new ModelAndView("auth/login");
        mav.addObject("shell", ShellViewModelFactory.playerShell(messageSource, locale, "/login"));
        mav.addObject("loginEmail", email == null ? "" : email);
        mav.addObject("loginError", loginErrorMessage(error, locale));
        mav.addObject("showResendVerification", "verify".equalsIgnoreCase(error));
        mav.addObject("verificationConfirmed", "1".equals(verified));
        mav.addObject("passwordResetCompleted", "1".equals(reset));
        mav.addObject("loggedOut", "1".equals(logout));
        mav.addObject("loginContinue", continueFlag != null);
        return mav;
    }

    @GetMapping("/register")
    public ModelAndView showRegister(final Locale locale) {
        if (CurrentAuthenticatedUser.get().isPresent()) {
            return new ModelAndView("redirect:/");
        }
        return registerView(new RegisterForm(), locale);
    }

    @PostMapping("/register")
    public ModelAndView register(
            @Valid @ModelAttribute("registerForm") final RegisterForm registerForm,
            final BindingResult bindingResult,
            final Locale locale) {
        if (!bindingResult.hasFieldErrors("password")
                && !bindingResult.hasFieldErrors("confirmPassword")
                && !registerForm.getPassword().equals(registerForm.getConfirmPassword())) {
            bindingResult.rejectValue(
                    "confirmPassword",
                    "auth.validation.passwordMismatch",
                    messageSource.getMessage("auth.validation.passwordMismatch", null, locale));
        }

        if (bindingResult.hasErrors()) {
            return registerView(registerForm, locale);
        }

        try {
            final VerificationRequestResult result =
                    accountAuthService.register(
                            new RegisterAccountRequest(
                                    registerForm.getEmail(),
                                    registerForm.getUsername(),
                                    registerForm.getName(),
                                    registerForm.getLastName(),
                                    registerForm.getPhone(),
                                    registerForm.getPassword()));
            return checkEmailView(
                    locale,
                    messageSource.getMessage(
                            "auth.checkEmail.registration.summary",
                            new Object[] {result.getEmail()},
                            locale),
                    "/login",
                    messageSource.getMessage("auth.backToLogin", null, locale),
                    messageSource.getMessage("auth.registration.requested", null, locale),
                    result.getExpiresAt());
        } catch (final AccountRegistrationException exception) {
            applyRegistrationError(bindingResult, exception);
            return registerView(registerForm, locale);
        }
    }

    @PostMapping("/register/resend-verification")
    public ModelAndView resendVerification(
            @RequestParam(value = "email", required = false) final String email,
            final Locale locale) {
        Optional<VerificationRequestResult> result = Optional.empty();
        try {
            if (email != null && !email.isBlank()) {
                result = accountAuthService.resendVerification(email);
            }
        } catch (final IllegalArgumentException ignored) {
            result = Optional.empty();
        }

        return checkEmailView(
                locale,
                messageSource.getMessage("auth.checkEmail.resend.summary", null, locale),
                "/login",
                messageSource.getMessage("auth.backToLogin", null, locale),
                messageSource.getMessage("auth.resendVerification.requested", null, locale),
                result.map(VerificationRequestResult::getExpiresAt).orElse(null));
    }

    @GetMapping("/forgot-password")
    public ModelAndView showForgotPassword(final Locale locale) {
        if (CurrentAuthenticatedUser.get().isPresent()) {
            return new ModelAndView("redirect:/");
        }
        return forgotPasswordView(new ForgotPasswordForm(), locale);
    }

    @PostMapping("/forgot-password")
    public ModelAndView requestPasswordReset(
            @Valid @ModelAttribute("forgotPasswordForm")
                    final ForgotPasswordForm forgotPasswordForm,
            final BindingResult bindingResult,
            final Locale locale) {
        if (bindingResult.hasErrors()) {
            return forgotPasswordView(forgotPasswordForm, locale);
        }

        final Optional<VerificationRequestResult> result =
                accountAuthService.requestPasswordReset(forgotPasswordForm.getEmail());
        return checkEmailView(
                locale,
                messageSource.getMessage("auth.checkEmail.passwordReset.summary", null, locale),
                "/login",
                messageSource.getMessage("auth.backToLogin", null, locale),
                messageSource.getMessage("auth.passwordReset.requested", null, locale),
                result.map(VerificationRequestResult::getExpiresAt).orElse(null));
    }

    private ModelAndView registerView(final RegisterForm registerForm, final Locale locale) {
        final ModelAndView mav = new ModelAndView("auth/register");
        mav.addObject(
                "shell", ShellViewModelFactory.playerShell(messageSource, locale, "/register"));
        mav.addObject("registerForm", registerForm);
        return mav;
    }

    private ModelAndView forgotPasswordView(
            final ForgotPasswordForm forgotPasswordForm, final Locale locale) {
        final ModelAndView mav = new ModelAndView("auth/forgot-password");
        mav.addObject(
                "shell",
                ShellViewModelFactory.playerShell(messageSource, locale, "/forgot-password"));
        mav.addObject("forgotPasswordForm", forgotPasswordForm);
        return mav;
    }

    private ModelAndView checkEmailView(
            final Locale locale,
            final String summary,
            final String backHref,
            final String actionLabel,
            final String eyebrow,
            final Instant expiresAt) {
        final ModelAndView mav = new ModelAndView("verification/check-email");
        mav.addObject(
                "shell",
                ShellViewModelFactory.playerShell(
                        messageSource, locale, "/verification/check-email"));
        mav.addObject("title", messageSource.getMessage("verification.checkEmail", null, locale));
        mav.addObject("summary", summary);
        mav.addObject("backHref", backHref);
        mav.addObject("actionLabel", actionLabel);
        mav.addObject("eyebrow", eyebrow);
        if (expiresAt != null) {
            mav.addObject(
                    "expiresAtLabel",
                    VerificationViews.expiryFormatter(locale)
                            .format(expiresAt.atZone(ZoneId.systemDefault())));
        }
        return mav;
    }

    private String loginErrorMessage(final String error, final Locale locale) {
        if ("verify".equalsIgnoreCase(error)) {
            return messageSource.getMessage("auth.login.error.verify", null, locale);
        }
        if ("set-password".equalsIgnoreCase(error)) {
            return messageSource.getMessage("auth.login.error.passwordSetup", null, locale);
        }
        if ("invalid".equalsIgnoreCase(error)) {
            return messageSource.getMessage("auth.login.error.invalid", null, locale);
        }
        return null;
    }

    private void applyRegistrationError(
            final BindingResult bindingResult, final AccountRegistrationException exception) {
        final String code = exception.getCode();
        if ("email_taken".equals(code) || "email_pending_verification".equals(code)) {
            bindingResult.rejectValue("email", code, exception.getMessage());
            return;
        }
        if ("username_taken".equals(code) || "username_invalid".equals(code)) {
            bindingResult.rejectValue("username", code, exception.getMessage());
            return;
        }
        if ("password_invalid".equals(code)) {
            bindingResult.rejectValue("password", code, exception.getMessage());
            return;
        }
        if ("name_invalid".equals(code)) {
            bindingResult.rejectValue("name", code, exception.getMessage());
            return;
        }
        if ("lastName_invalid".equals(code)) {
            bindingResult.rejectValue("lastName", code, exception.getMessage());
            return;
        }
        if ("phone_invalid".equals(code)) {
            bindingResult.rejectValue("phone", code, exception.getMessage());
        }
    }
}
