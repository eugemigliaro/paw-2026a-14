package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.services.exceptions.AccountRegistrationException;
import ar.edu.itba.paw.webapp.form.AccountProfileForm;
import ar.edu.itba.paw.webapp.security.AuthenticatedUserPrincipal;
import ar.edu.itba.paw.webapp.viewmodel.ShellViewModelFactory;
import java.util.Locale;
import javax.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class AccountController {

    private final UserService userService;
    private final MessageSource messageSource;

    public AccountController(final UserService userService, final MessageSource messageSource) {
        this.userService = userService;
        this.messageSource = messageSource;
    }

    @ModelAttribute("accountProfileForm")
    public AccountProfileForm accountProfileForm() {
        return new AccountProfileForm();
    }

    @GetMapping(value = "/account", params = "!updated")
    public ModelAndView showAccount(final Locale locale) {
        return accountView(requiredAuthenticatedUser(), locale, false);
    }

    @GetMapping(value = "/account", params = "updated=1")
    public ModelAndView showUpdatedAccount(final Locale locale) {
        return accountView(requiredAuthenticatedUser(), locale, true);
    }

    @GetMapping("/account/edit")
    public ModelAndView showEditAccount(final Locale locale) {
        final User user = requiredAuthenticatedUser();
        return accountEditView(accountProfileFormFrom(user), user.getEmail(), locale);
    }

    @PostMapping("/account/edit")
    public ModelAndView updateAccount(
            @Valid @ModelAttribute("accountProfileForm")
                    final AccountProfileForm accountProfileForm,
            final BindingResult bindingResult,
            final Locale locale) {
        final Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        final AuthenticatedUserPrincipal principal =
                (AuthenticatedUserPrincipal) authentication.getPrincipal();
        final User currentUser = requiredAuthenticatedUser();

        if (bindingResult.hasErrors()) {
            return accountEditView(accountProfileForm, currentUser.getEmail(), locale);
        }

        try {
            final User updatedUser =
                    userService.updateProfile(
                            principal.getUserId(),
                            accountProfileForm.getUsername(),
                            accountProfileForm.getName(),
                            accountProfileForm.getLastName(),
                            accountProfileForm.getPhone());
            SecurityContextHolder.getContext()
                    .setAuthentication(
                            new UsernamePasswordAuthenticationToken(
                                    new AuthenticatedUserPrincipal(
                                            updatedUser, principal.getRole()),
                                    authentication.getCredentials(),
                                    authentication.getAuthorities()));
            return new ModelAndView("redirect:/account?updated=1");
        } catch (final AccountRegistrationException exception) {
            applyProfileUpdateError(bindingResult, exception);
            return accountEditView(accountProfileForm, currentUser.getEmail(), locale);
        }
    }

    private ModelAndView accountView(final User user, final Locale locale, final boolean updated) {
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
                        "Review your Match Point profile details and current session.",
                        locale));
        mav.addObject(
                "accountEditLabel",
                messageSource.getMessage("account.edit", null, "Edit profile", locale));
        mav.addObject(
                "logoutLabel", messageSource.getMessage("nav.logout", null, "Logout", locale));
        mav.addObject(
                "accountUpdated",
                updated
                        ? messageSource.getMessage(
                                "account.updated", null, "Your profile was updated.", locale)
                        : null);
        mav.addObject("accountProfile", user);
        return mav;
    }

    private ModelAndView accountEditView(
            final AccountProfileForm accountProfileForm,
            final String accountEmail,
            final Locale locale) {
        final ModelAndView mav = new ModelAndView("account/edit");
        mav.addObject(
                "pageTitle",
                messageSource.getMessage(
                        "page.title.accountEdit", null, "Match Point | Edit Profile", locale));
        mav.addObject(
                "shell", ShellViewModelFactory.playerShell(messageSource, locale, "/account"));
        mav.addObject(
                "accountTitle",
                messageSource.getMessage("account.editTitle", null, "Edit profile", locale));
        mav.addObject(
                "accountDescription",
                messageSource.getMessage(
                        "account.editDescription",
                        null,
                        "Update the profile details shown across your Match Point account.",
                        locale));
        mav.addObject(
                "accountSaveLabel",
                messageSource.getMessage("account.save", null, "Save changes", locale));
        mav.addObject(
                "accountCancelLabel",
                messageSource.getMessage("common.cancel", null, "Cancel", locale));
        mav.addObject("accountEmail", accountEmail);
        mav.addObject("accountProfileForm", accountProfileForm);
        return mav;
    }

    private User requiredAuthenticatedUser() {
        final AuthenticatedUserPrincipal principal =
                (AuthenticatedUserPrincipal)
                        SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userService
                .findById(principal.getUserId())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }

    private static AccountProfileForm accountProfileFormFrom(final User user) {
        final AccountProfileForm form = new AccountProfileForm();
        form.setUsername(user.getUsername());
        form.setName(user.getName());
        form.setLastName(user.getLastName());
        form.setPhone(user.getPhone());
        return form;
    }

    private void applyProfileUpdateError(
            final BindingResult bindingResult, final AccountRegistrationException exception) {
        final String code = exception.getCode();
        if ("username_taken".equals(code) || "username_invalid".equals(code)) {
            bindingResult.rejectValue("username", code, exception.getMessage());
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
