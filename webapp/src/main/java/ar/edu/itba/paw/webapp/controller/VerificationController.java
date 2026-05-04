package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.UserAccount;
import ar.edu.itba.paw.models.UserRole;
import ar.edu.itba.paw.services.AccountAuthService;
import ar.edu.itba.paw.services.VerificationConfirmationResult;
import ar.edu.itba.paw.services.VerificationPreview;
import ar.edu.itba.paw.services.exceptions.VerificationFailureException;
import ar.edu.itba.paw.webapp.security.AuthenticatedUserPrincipal;
import ar.edu.itba.paw.webapp.utils.VerificationViews;
import ar.edu.itba.paw.webapp.viewmodel.ShellViewModelFactory;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class VerificationController {

    private final AccountAuthService accountAuthService;
    private final MessageSource messageSource;
    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

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
            mav.addObject(
                    "shell",
                    ShellViewModelFactory.playerShell(messageSource, locale, "/verifications"));
            mav.addObject("preview", preview);
            mav.addObject("confirmPath", "/verifications/" + token + "/confirm");
            mav.addObject(
                    "expiresAtLabel",
                    VerificationViews.expiryFormatter(locale)
                            .format(preview.getExpiresAt().atZone(ZoneId.systemDefault())));
            return mav;
        } catch (final VerificationFailureException exception) {
            return buildErrorView(exception, locale);
        }
    }

    @PostMapping("/verifications/{token}/confirm")
    public ModelAndView confirm(
            @PathVariable("token") final String token,
            final Locale locale,
            final HttpServletRequest request,
            final HttpServletResponse response) {
        try {
            final VerificationConfirmationResult result =
                    accountAuthService.confirmVerification(token);
            result.getAccount()
                    .ifPresent(account -> authenticateVerifiedAccount(account, request, response));
            return new ModelAndView("redirect:" + result.getRedirectUrl());
        } catch (final VerificationFailureException exception) {
            return buildErrorView(exception, locale);
        }
    }

    private ModelAndView buildErrorView(
            final VerificationFailureException exception, final Locale locale) {
        return VerificationViews.buildErrorView(exception, messageSource, locale, "/");
    }

    private void authenticateVerifiedAccount(
            final UserAccount account,
            final HttpServletRequest request,
            final HttpServletResponse response) {
        final SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new AuthenticatedUserPrincipal(account),
                        null,
                        authoritiesFor(account.getRole())));
        SecurityContextHolder.setContext(securityContext);
        securityContextRepository.saveContext(securityContext, request, response);
    }

    private static List<GrantedAuthority> authoritiesFor(final UserRole role) {
        if (role != null && role.isAdmin()) {
            return List.of(
                    new SimpleGrantedAuthority("ROLE_ADMIN_MOD"),
                    new SimpleGrantedAuthority("ROLE_USER"));
        }
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }
}
