package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.services.exceptions.AccountRegistrationException;
import ar.edu.itba.paw.services.exceptions.ImageUploadException;
import ar.edu.itba.paw.webapp.form.AccountProfileForm;
import ar.edu.itba.paw.webapp.security.AuthenticatedUserPrincipal;
import ar.edu.itba.paw.webapp.security.CurrentAuthenticatedUser;
import ar.edu.itba.paw.webapp.utils.ImageUrlHelper;
import ar.edu.itba.paw.webapp.viewmodel.ShellViewModelFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import javax.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
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
        return accountEditView(accountProfileFormFrom(user), user.getEmail(), null, locale);
    }

    @PostMapping("/account/edit")
    public ModelAndView updateAccount(
            @Valid @ModelAttribute("accountProfileForm")
                    final AccountProfileForm accountProfileForm,
            final BindingResult bindingResult,
            final Locale locale) {
        final Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        final AuthenticatedUserPrincipal principal = requiredAuthenticatedPrincipal();
        final User currentUser = requiredAuthenticatedUser();

        if (bindingResult.hasErrors()) {
            return accountEditView(accountProfileForm, currentUser.getEmail(), null, locale);
        }

        try (InputStream profileImageStream =
                accountProfileForm.getProfileImage() == null
                                || accountProfileForm.getProfileImage().isEmpty()
                        ? null
                        : accountProfileForm.getProfileImage().getInputStream()) {
            final User updatedUser =
                    userService.updateProfile(
                            principal.getUserId(),
                            accountProfileForm.getUsername(),
                            accountProfileForm.getName(),
                            accountProfileForm.getLastName(),
                            accountProfileForm.getPhone(),
                            accountProfileForm.getProfileImage() == null
                                    ? null
                                    : accountProfileForm.getProfileImage().getContentType(),
                            accountProfileForm.getProfileImage() == null
                                    ? 0L
                                    : accountProfileForm.getProfileImage().getSize(),
                            profileImageStream);
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
            return accountEditView(accountProfileForm, currentUser.getEmail(), null, locale);
        } catch (final ImageUploadException exception) {
            return accountEditView(
                    accountProfileForm,
                    currentUser.getEmail(),
                    profileImageError(exception, locale),
                    locale);
        } catch (final IllegalArgumentException exception) {
            return accountEditView(
                    accountProfileForm, currentUser.getEmail(), profileImageError(locale), locale);
        } catch (final IOException exception) {
            return accountEditView(
                    accountProfileForm,
                    currentUser.getEmail(),
                    profileImageError(
                            "account.profileImage.error.unavailable",
                            "We could not process the uploaded image. Please try again.",
                            locale),
                    locale);
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
        addProfileImageObjects(mav, user, locale);
        mav.addObject("accountProfile", user);
        return mav;
    }

    private ModelAndView accountEditView(
            final AccountProfileForm accountProfileForm,
            final String accountEmail,
            final String profileImageError,
            final Locale locale) {
        final ModelAndView mav = new ModelAndView("account/edit");
        final User user = requiredAuthenticatedUser();
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
        mav.addObject(
                "accountProfileImageTitle",
                messageSource.getMessage(
                        "account.profileImage.title", null, "Profile picture", locale));
        mav.addObject(
                "accountProfileImageDescription",
                messageSource.getMessage(
                        "account.profileImage.description",
                        null,
                        "Upload a photo shown on your account and public profile.",
                        locale));
        mav.addObject(
                "accountProfileImageHint",
                messageSource.getMessage(
                        "account.profileImage.hint",
                        null,
                        "Accepted formats: JPG, PNG, WEBP, GIF. Max size 5 MB.",
                        locale));
        mav.addObject(
                "accountProfileImageError", profileImageError != null ? profileImageError : null);
        mav.addObject("accountEmail", accountEmail);
        mav.addObject("accountProfileForm", accountProfileForm);
        addProfileImageObjects(mav, user, locale);
        return mav;
    }

    private User requiredAuthenticatedUser() {
        final AuthenticatedUserPrincipal principal = requiredAuthenticatedPrincipal();
        return userService
                .findById(principal.getUserId())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }

    private static AuthenticatedUserPrincipal requiredAuthenticatedPrincipal() {
        return CurrentAuthenticatedUser.get()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
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

    private void addProfileImageObjects(
            final ModelAndView mav, final User user, final Locale locale) {
        mav.addObject("accountProfileImageUrl", ImageUrlHelper.profileUrlFor(user));
        mav.addObject(
                "accountProfileImageAlt",
                messageSource.getMessage(
                        "account.profileImage.alt",
                        new Object[] {user.getUsername()},
                        user.getUsername() + " profile picture",
                        locale));
    }

    private String profileImageError(
            final String code, final String defaultMessage, final Locale locale) {
        return messageSource.getMessage(code, null, defaultMessage, locale);
    }

    private String profileImageError(final ImageUploadException exception, final Locale locale) {
        if (ImageUploadException.UNSUPPORTED_FORMAT.equals(exception.getCode())) {
            return messageSource.getMessage(
                    "account.profileImage.error.invalidFormat",
                    null,
                    "Please upload a JPG, PNG, WEBP, or GIF image.",
                    locale);
        }
        if (ImageUploadException.EMPTY_FILE.equals(exception.getCode())) {
            return messageSource.getMessage(
                    "account.profileImage.error.empty",
                    null,
                    "The uploaded image is empty.",
                    locale);
        }
        if (ImageUploadException.TOO_LARGE.equals(exception.getCode())) {
            return messageSource.getMessage(
                    "account.profileImage.error.tooLarge",
                    null,
                    "The uploaded image must be 5 MB or smaller.",
                    locale);
        }

        return messageSource.getMessage(
                "account.update.error.unavailable",
                null,
                "We could not update your profile. Please try again.",
                locale);
    }

    private String profileImageError(final Locale locale) {
        return messageSource.getMessage(
                "account.update.error.unavailable",
                null,
                "We could not update your profile. Please try again.",
                locale);
    }
}
