package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.security.CurrentAuthenticatedUser;
import ar.edu.itba.paw.webapp.utils.ImageUrlHelper;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.PublicProfilePageViewModel;
import ar.edu.itba.paw.webapp.viewmodel.ShellViewModelFactory;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class PublicProfileController {

    private final UserService userService;
    private final MessageSource messageSource;

    public PublicProfileController(
            final UserService userService, final MessageSource messageSource) {
        this.userService = userService;
        this.messageSource = messageSource;
    }

    @GetMapping("/users/{username}")
    public ModelAndView showPublicProfile(
            @PathVariable("username") final String username, final Locale locale) {
        final User user =
                userService
                        .findByUsername(username)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        final ModelAndView mav = new ModelAndView("users/profile");
        mav.addObject(
                "pageTitle",
                messageSource.getMessage(
                        "page.title.publicProfile",
                        new Object[] {user.getUsername()},
                        "Match Point | " + user.getUsername(),
                        locale));
        mav.addObject("shell", ShellViewModelFactory.playerShell(messageSource, locale));
        mav.addObject(
                "profilePage",
                new PublicProfilePageViewModel(
                        user.getUsername(), user.getEmail(), ImageUrlHelper.profileUrlFor(user)));
        mav.addObject(
                "profileEyebrow",
                messageSource.getMessage(
                        "profile.public.eyebrow", null, "Community profile", locale));
        mav.addObject(
                "profileTitle",
                messageSource.getMessage("profile.public.title", null, "Public profile", locale));
        mav.addObject(
                "profileDescription",
                messageSource.getMessage(
                        "profile.public.description",
                        null,
                        "See this Match Point member's public identity and contact email.",
                        locale));
        mav.addObject(
                "profileImageAlt",
                messageSource.getMessage(
                        "profile.public.avatarAlt",
                        new Object[] {user.getUsername()},
                        user.getUsername() + " profile picture",
                        locale));
        mav.addObject(
                "profileUsernameLabel",
                messageSource.getMessage("profile.public.username", null, "Username", locale));
        mav.addObject(
                "profileEmailLabel",
                messageSource.getMessage("profile.public.email", null, "Email", locale));
        CurrentAuthenticatedUser.get()
                .filter(principal -> principal.getUserId().equals(user.getId()))
                .ifPresent(
                        principal -> {
                            mav.addObject("profileEditHref", "/account/edit");
                            mav.addObject(
                                    "profileEditLabel",
                                    messageSource.getMessage(
                                            "profile.public.edit", null, "Edit profile", locale));
                        });
        return mav;
    }
}
