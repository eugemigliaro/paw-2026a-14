package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.ImageUpload;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.services.exceptions.imageUpload.ImageUploadException;
import ar.edu.itba.paw.webapp.form.AccountProfileForm;
import ar.edu.itba.paw.webapp.security.annotation.AuthenticatedUser;
import ar.edu.itba.paw.webapp.utils.ImageUrlHelper;
import ar.edu.itba.paw.webapp.utils.SecurityControllerUtils;
import java.io.IOException;
import java.util.Locale;
import javax.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    @GetMapping("/account")
    public ModelAndView showAccount(
            @AuthenticatedUser final User user, final Model model, final Locale locale) {
        return accountView(
                user,
                locale,
                Boolean.TRUE.equals(model.asMap().get("accountUpdated")),
                accountProfileFormFrom(user),
                null);
    }

    @PostMapping("/account/edit")
    public ModelAndView updateAccount(
            @AuthenticatedUser final User user,
            @Valid @ModelAttribute("accountProfileForm")
                    final AccountProfileForm accountProfileForm,
            final BindingResult bindingResult,
            final RedirectAttributes redirectAttributes,
            final Locale locale) {

        final User existingUsername =
                userService.findByUsername(accountProfileForm.getUsername()).orElse(null);
        if (existingUsername != null && !existingUsername.getId().equals(user.getId())) {
            bindingResult.rejectValue("username", "auth.registration.error.usernameTaken");
        }

        if (bindingResult.hasErrors()) {
            return accountView(user, locale, false, accountProfileForm, null);
        }

        try {
            final User updatedUser =
                    userService.updateProfile(
                            user,
                            accountProfileForm.getUsername(),
                            accountProfileForm.getName(),
                            accountProfileForm.getLastName(),
                            accountProfileForm.getPhone(),
                            profileImageUpload(accountProfileForm.getProfileImage()));
            SecurityControllerUtils.refreshAuthentication(updatedUser);
            redirectAttributes.addFlashAttribute("accountUpdated", true);
            return new ModelAndView("redirect:/account");
        } catch (final IOException e) {
            throw new ImageUploadException("account.profileImage.error.uploadFailed");
        }
    }

    private ModelAndView accountView(
            final User user,
            final Locale locale,
            final boolean updated,
            final AccountProfileForm accountProfileForm,
            final String
                    profileImageError) { // TODO: remove messageSource and instead send msg keys to
        // the view. Resolve them there with <spring:message>.
        final ModelAndView mav = new ModelAndView("account/index");
        mav.addObject(
                "pageTitle",
                messageSource.getMessage(
                        "page.title.account", null, "Match Point | Account", locale));
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
                "accountProfileImageHint",
                messageSource.getMessage(
                        "account.profileImage.hint",
                        null,
                        "Accepted formats: JPG, PNG, WEBP, GIF. Max size 5 MB.",
                        locale));
        mav.addObject("accountProfileImageError", profileImageError);
        mav.addObject(
                "logoutLabel", messageSource.getMessage("nav.logout", null, "Logout", locale));
        mav.addObject(
                "accountUpdated",
                updated
                        ? messageSource.getMessage(
                                "account.updated", null, "Your profile was updated.", locale)
                        : null);
        mav.addObject("accountProfileImageUrl", ImageUrlHelper.profileUrlFor(user));
        mav.addObject(
                "accountProfileImageAlt",
                messageSource.getMessage(
                        "account.profileImage.alt",
                        new Object[] {user.getUsername()},
                        user.getUsername() + " profile picture",
                        locale));
        mav.addObject("accountProfile", user);
        mav.addObject("accountProfileForm", accountProfileForm);
        mav.addObject("accountEmail", user.getEmail());
        mav.addObject("accountPublicProfileHref", "/users/" + user.getUsername());
        mav.addObject(
                "accountPublicProfileLabel",
                messageSource.getMessage(
                        "account.viewPublicProfile", null, "View public profile", locale));
        return mav;
    }

    private static AccountProfileForm accountProfileFormFrom(final User user) {
        final AccountProfileForm form = new AccountProfileForm();
        form.setUsername(user.getUsername());
        form.setName(user.getName());
        form.setLastName(user.getLastName());
        form.setPhone(user.getPhone());
        return form;
    }

    private ImageUpload profileImageUpload(
            final MultipartFile
                    profileImage) { // TODO: move to a different file. It's also used in other
        // controllers.
        if (profileImage == null) {
            return null;
        }
        return new ImageUpload() {
            @Override
            public String getContentType() {
                return profileImage.getContentType();
            }

            @Override
            public long getContentLength() {
                return profileImage.getSize();
            }

            @Override
            public java.io.InputStream getContentStream() throws java.io.IOException {
                return profileImage.getInputStream();
            }
        };
    }
}
