package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.PlatformTime;
import ar.edu.itba.paw.services.AccountAuthService;
import ar.edu.itba.paw.services.RegisterAccountRequest;
import ar.edu.itba.paw.services.VerificationRequestResult;
import ar.edu.itba.paw.webapp.form.ForgotPasswordForm;
import ar.edu.itba.paw.webapp.form.RegisterForm;
import ar.edu.itba.paw.webapp.security.CurrentAuthenticatedUser;
import ar.edu.itba.paw.webapp.utils.SecurityControllerUtils;
import ar.edu.itba.paw.webapp.utils.VerificationViews;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthController.class);
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
    public ModelAndView
            showLogin( // TODO: consider typing these flags as boolean. "1".equals(flag) is a bit
                    // weird
                    @RequestParam(value = "error", required = false) final String error,
                    @RequestParam(value = "email", required = false) final String email,
                    @RequestParam(value = "verified", required = false) final String verified,
                    @RequestParam(value = "reset", required = false) final String reset,
                    @RequestParam(value = "logout", required = false) final String logout,
                    @RequestParam(value = "continue", required = false) final String continueFlag,
                    final Locale locale) {
        if (SecurityControllerUtils.currentUserOrNull()
                != null) { // TODO: redundant with SecurityConfig? check and remove if so
            LOGGER.debug("Authenticated user redirected from /login");
            return new ModelAndView("redirect:/");
        }

        final ModelAndView mav = new ModelAndView("auth/login");
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
        if (SecurityControllerUtils.currentUserOrNull()
                != null) { // TODO: redundant with SecurityConfig? check and remove if so
            LOGGER.debug("Authenticated user redirected from /register");
            return new ModelAndView("redirect:/");
        }
        return registerView(new RegisterForm(), locale);
    }

    @PostMapping("/register")
    public ModelAndView register(
            @Valid @ModelAttribute("registerForm") final RegisterForm registerForm,
            final BindingResult bindingResult,
            final Locale locale) {
        if (bindingResult.hasErrors()) {
            return registerView(registerForm, locale);
        }

        final VerificationRequestResult result =
                accountAuthService.register(
                        new RegisterAccountRequest(
                                registerForm.getEmail(),
                                registerForm.getUsername(),
                                registerForm.getName(),
                                registerForm.getLastName(),
                                registerForm.getPhone(),
                                registerForm.getPassword()));
        LOGGER.info("Registration verification requested locale={}", locale);
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
    }

    @PostMapping("/register/resend-verification")
    public ModelAndView resendVerification(
            @RequestParam(value = "email", required = false) final String email,
            final Locale locale) {
        Optional<VerificationRequestResult> result = Optional.empty();
        try {
            if (email != null && !email.isBlank()) {
                result = accountAuthService.resendVerification(email);
                LOGGER.info("Resend verification requested locale={}", locale);
            }
        } catch (final IllegalArgumentException ignored) {
            LOGGER.debug("Resend verification rejected due to invalid email format");
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
            LOGGER.debug(
                    "Authenticated user redirected from /forgot-password"); // TODO: redundant with
            // SecurityConfig? check
            // and remove if so
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
        LOGGER.info("Password reset requested locale={}", locale);
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
        mav.addObject("registerForm", registerForm);
        return mav;
    }

    private ModelAndView forgotPasswordView(
            final ForgotPasswordForm forgotPasswordForm, final Locale locale) {
        final ModelAndView mav = new ModelAndView("auth/forgot-password");
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
        mav.addObject("title", messageSource.getMessage("verification.checkEmail", null, locale));
        mav.addObject("summary", summary);
        mav.addObject("backHref", backHref);
        mav.addObject("actionLabel", actionLabel);
        mav.addObject("eyebrow", eyebrow);
        if (expiresAt != null) {
            mav.addObject(
                    "expiresAtLabel",
                    VerificationViews.expiryFormatter(locale)
                            .format(expiresAt.atZone(PlatformTime.ZONE)));
        }
        return mav;
    }

    // TODO: consider changing the errors to "verify", "passwordSetup" and "invalid", and removing
    // the messageSource from this class.
    // That way we can send "auth.login.error." + error as the msg key and resolve it in the view.
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
}
