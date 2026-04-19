package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.webapp.security.AuthenticatedUserPrincipal;
import ar.edu.itba.paw.webapp.viewmodel.ShellViewModelFactory;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class AccountController {

    private final MessageSource messageSource;

    public AccountController(final MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @GetMapping("/account")
    public ModelAndView showAccount(final Locale locale) {
        final AuthenticatedUserPrincipal principal =
                (AuthenticatedUserPrincipal)
                        SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        final ModelAndView mav = new ModelAndView("account/index");
        mav.addObject(
                "pageTitle",
                messageSource.getMessage(
                        "page.title.account", null, "Match Point | Account", locale));
        mav.addObject(
                "shell", ShellViewModelFactory.playerShell(messageSource, locale, "/account"));
        mav.addObject(
                "accountTitle", messageSource.getMessage("account.title", null, "Account", locale));
        mav.addObject(
                "accountDescription",
                messageSource.getMessage(
                        "account.description",
                        null,
                        "Manage your current Match Point session and account access.",
                        locale));
        mav.addObject(
                "accountUsernameLabel",
                messageSource.getMessage("account.username", null, "Username", locale));
        mav.addObject(
                "accountEmailLabel",
                messageSource.getMessage("account.email", null, "Email", locale));
        mav.addObject(
                "logoutLabel", messageSource.getMessage("nav.logout", null, "Logout", locale));
        mav.addObject("username", principal.getName());
        mav.addObject("email", principal.getEmail());
        return mav;
    }
}
