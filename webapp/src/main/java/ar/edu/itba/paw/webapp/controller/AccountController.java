package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.exceptions.imageUpload.ImageUploadException;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.form.AccountProfileForm;
import ar.edu.itba.paw.webapp.security.annotation.AuthenticatedUser;
import ar.edu.itba.paw.webapp.utils.ImageUrlHelper;
import ar.edu.itba.paw.webapp.utils.MultipartImageUpload;
import ar.edu.itba.paw.webapp.utils.SecurityControllerUtils;
import java.io.IOException;
import javax.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AccountController {

    private final UserService userService;

    public AccountController(final UserService userService) {
        this.userService = userService;
    }

    @ModelAttribute("accountProfileForm")
    public AccountProfileForm accountProfileForm() {
        return new AccountProfileForm();
    }

    @GetMapping("/account")
    public ModelAndView showAccount(@AuthenticatedUser final User user, final Model model) {
        return accountView(
                user,
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
            final RedirectAttributes redirectAttributes) {

        final User existingUsername =
                userService.findByUsername(accountProfileForm.getUsername()).orElse(null);
        if (existingUsername != null && !existingUsername.getId().equals(user.getId())) {
            bindingResult.rejectValue("username", "auth.registration.error.usernameTaken");
        }

        if (bindingResult.hasErrors()) {
            return accountView(user, false, accountProfileForm, null);
        }

        try {
            final User updatedUser =
                    userService.updateProfile(
                            user,
                            accountProfileForm.getUsername(),
                            accountProfileForm.getName(),
                            accountProfileForm.getLastName(),
                            accountProfileForm.getPhone(),
                            MultipartImageUpload.from(accountProfileForm.getProfileImage()));
            SecurityControllerUtils.refreshAuthentication(updatedUser);
            redirectAttributes.addFlashAttribute("accountUpdated", true);
            return new ModelAndView("redirect:/account");
        } catch (final IOException e) {
            throw new ImageUploadException("account.profileImage.error.uploadFailed");
        }
    }

    private ModelAndView accountView(
            final User user,
            final boolean updated,
            final AccountProfileForm accountProfileForm,
            final String profileImageError) {
        final ModelAndView mav = new ModelAndView("account/index");
        mav.addObject("accountUpdated", updated);
        mav.addObject("accountProfileImageError", profileImageError);
        mav.addObject("accountProfileImageUrl", ImageUrlHelper.profileUrlFor(user));
        mav.addObject("accountProfile", user);
        mav.addObject("accountProfileForm", accountProfileForm);
        mav.addObject("accountEmail", user.getEmail());
        mav.addObject("accountPublicProfileHref", "/users/" + user.getUsername());
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
}
