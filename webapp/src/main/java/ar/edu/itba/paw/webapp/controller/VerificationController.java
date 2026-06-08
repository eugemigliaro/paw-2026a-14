package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.PlatformTime;
import ar.edu.itba.paw.services.AccountAuthService;
import ar.edu.itba.paw.services.VerificationConfirmationResult;
import ar.edu.itba.paw.services.VerificationPreview;
import ar.edu.itba.paw.webapp.utils.SecurityControllerUtils;
import ar.edu.itba.paw.webapp.utils.VerificationViews;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class VerificationController {

    private final AccountAuthService accountAuthService;

    @Autowired
    public VerificationController(final AccountAuthService accountAuthService) {
        this.accountAuthService = accountAuthService;
    }

    @GetMapping("/verifications/{token}")
    public ModelAndView showVerification(
            @PathVariable("token") final String token, final Locale locale) {
        final VerificationPreview preview = accountAuthService.getVerificationPreview(token);
        final ModelAndView mav = new ModelAndView("verification/confirm");
        mav.addObject("preview", preview);
        mav.addObject("confirmPath", "/verifications/" + token + "/confirm");
        mav.addObject(
                "expiresAtLabel",
                VerificationViews.expiryFormatter(locale)
                        .format(preview.getExpiresAt().atZone(PlatformTime.ZONE)));
        return mav;
    }

    @PostMapping("/verifications/{token}/confirm")
    public ModelAndView confirm(
            @PathVariable("token") final String token,
            final Locale locale,
            final HttpServletRequest request,
            final HttpServletResponse response) {

        final VerificationConfirmationResult result = accountAuthService.confirmVerification(token);
        result.getAccount()
                .ifPresent(
                        account ->
                                SecurityControllerUtils.authenticateVerifiedAccount(
                                        account, request, response));
        return new ModelAndView("redirect:/");
    }
}
