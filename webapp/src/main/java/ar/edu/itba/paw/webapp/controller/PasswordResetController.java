package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.PlatformTime;
import ar.edu.itba.paw.services.AccountAuthService;
import ar.edu.itba.paw.services.PasswordResetPreview;
import ar.edu.itba.paw.webapp.form.ResetPasswordForm;
import ar.edu.itba.paw.webapp.utils.VerificationViews;
import java.util.Locale;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    public PasswordResetController(final AccountAuthService accountAuthService) {
        this.accountAuthService = accountAuthService;
    }

    @ModelAttribute("resetPasswordForm")
    public ResetPasswordForm resetPasswordForm() {
        return new ResetPasswordForm();
    }

    @GetMapping("/password-reset/{token}")
    public ModelAndView showPasswordReset(
            @PathVariable("token") final String token, final Locale locale) {
        return passwordResetView(
                token,
                accountAuthService.getPasswordResetPreview(token),
                new ResetPasswordForm(),
                locale);
    }

    @PostMapping("/password-reset/{token}")
    public ModelAndView resetPassword(
            @PathVariable("token") final String token,
            @Valid @ModelAttribute("resetPasswordForm") final ResetPasswordForm resetPasswordForm,
            final BindingResult bindingResult,
            final Locale locale) {

        if (bindingResult.hasErrors()) {
            return passwordResetView(
                    token,
                    accountAuthService.getPasswordResetPreview(token),
                    resetPasswordForm,
                    locale);
        }

        accountAuthService.resetPassword(token, resetPasswordForm.getPassword());
        return new ModelAndView("redirect:/login?reset=1");
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
                        .format(preview.getExpiresAt().atZone(PlatformTime.ZONE)));
        mav.addObject("resetPasswordForm", resetPasswordForm);
        return mav;
    }
}
