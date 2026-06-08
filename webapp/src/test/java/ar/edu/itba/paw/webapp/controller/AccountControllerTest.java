package ar.edu.itba.paw.webapp.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.ImageUpload;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.security.annotation.CurrentUserArgumentResolver;
import ar.edu.itba.paw.webapp.utils.AuthenticationUtils;
import ar.edu.itba.paw.webapp.validation.UserEmailValidator;
import ar.edu.itba.paw.webapp.validation.UsernameValidator;
import java.util.Locale;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

class AccountControllerTest {

    private MockMvc mockMvc;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = Mockito.mock(UserService.class);
        final MessageSource messageSource = messageSource();
        final UserEmailValidator userEmailValidator =
                new UserEmailValidator(Mockito.mock(UserService.class));
        final UsernameValidator usernameValidator =
                new UsernameValidator(Mockito.mock(UserService.class));

        mockMvc =
                MockMvcBuilders.standaloneSetup(new AccountController(userService))
                        .setCustomArgumentResolvers(new CurrentUserArgumentResolver())
                        .setValidator(
                                validator(messageSource, userEmailValidator, usernameValidator))
                        .setLocaleResolver(localeResolver())
                        .addInterceptors(localeChangeInterceptor())
                        .defaultRequest(get("/").locale(Locale.ENGLISH))
                        .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAccountRouteRendersPrivateAccountPageForAuthenticatedUsers() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host_player");

        mockMvc.perform(get("/account"))
                .andExpect(status().isOk())
                .andExpect(view().name("account/index"))
                .andExpect(model().attributeExists("accountProfile"))
                .andExpect(model().attributeExists("accountProfileForm"));
    }

    @Test
    void getAccountRouteRendersPrivateAccountPageUnderSpanishLocale() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host_player");

        mockMvc.perform(get("/account").param("lang", "es"))
                .andExpect(status().isOk())
                .andExpect(view().name("account/index"))
                .andExpect(model().attributeExists("accountProfile"));
    }

    @Test
    void getAccountRouteWithoutAuthenticatedUserReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/account")).andExpect(status().isUnauthorized());
    }

    @Test
    void postAccountEditRouteWithoutAuthenticatedUserReturnsUnauthorized() throws Exception {
        mockMvc.perform(
                        post("/account/edit")
                                .param("username", "updated_user")
                                .param("name", "Taylor")
                                .param("lastName", "Morgan")
                                .param("phone", ""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postAccountEditRouteUpdatesProfileAndRedirects() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host_player");
        Mockito.when(
                        userService.updateProfile(
                                ArgumentMatchers.any(User.class),
                                ArgumentMatchers.anyString(),
                                ArgumentMatchers.anyString(),
                                ArgumentMatchers.anyString(),
                                ArgumentMatchers.any(),
                                ArgumentMatchers.any()))
                .thenReturn(updatedUser());

        mockMvc.perform(
                        post("/account/edit")
                                .param("username", "updated_user")
                                .param("name", "Taylor")
                                .param("lastName", "Morgan")
                                .param("phone", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/account"))
                .andExpect(flash().attribute("accountUpdated", true));
    }

    @Test
    void postAccountEditRouteUpdatesPictureAndRedirects() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host_player");
        Mockito.when(
                        userService.updateProfile(
                                ArgumentMatchers.any(User.class),
                                ArgumentMatchers.anyString(),
                                ArgumentMatchers.anyString(),
                                ArgumentMatchers.anyString(),
                                ArgumentMatchers.any(),
                                ArgumentMatchers.any(ImageUpload.class)))
                .thenReturn(updatedUser());

        mockMvc.perform(
                        multipart("/account/edit")
                                .file(
                                        new MockMultipartFile(
                                                "profileImage",
                                                "avatar.png",
                                                "image/png",
                                                new byte[] {1, 2, 3}))
                                .param("username", "host_player")
                                .param("name", "Jamie")
                                .param("lastName", "Rivera")
                                .param("phone", "+1 555 123 4567"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/account"))
                .andExpect(flash().attribute("accountUpdated", true));
    }

    @Test
    void postAccountEditRouteShowsImageFieldErrorForUnsupportedFormat() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host_player");

        mockMvc.perform(
                        multipart("/account/edit")
                                .file(
                                        new MockMultipartFile(
                                                "profileImage",
                                                "avatar.pdf",
                                                "application/pdf",
                                                new byte[] {1, 2, 3}))
                                .locale(Locale.ENGLISH)
                                .param("username", "host_player")
                                .param("name", "Jamie")
                                .param("lastName", "Rivera")
                                .param("phone", "+1 555 123 4567"))
                .andExpect(status().isOk())
                .andExpect(view().name("account/index"))
                .andExpect(model().attributeHasFieldErrors("accountProfileForm", "profileImage"));
    }

    private static User updatedUser() {
        return new User(
                9L,
                "host@test.com",
                "host_player",
                "Jamie",
                "Rivera",
                "+1 555 123 4567",
                null,
                null);
    }

    private static MessageSource messageSource() {
        final ReloadableResourceBundleMessageSource messageSource =
                new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:i18n/messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);
        return messageSource;
    }

    private static SessionLocaleResolver localeResolver() {
        final SessionLocaleResolver localeResolver = new SessionLocaleResolver();
        localeResolver.setDefaultLocale(Locale.ENGLISH);
        return localeResolver;
    }

    private static LocaleChangeInterceptor localeChangeInterceptor() {
        final LocaleChangeInterceptor localeChangeInterceptor = new LocaleChangeInterceptor();
        localeChangeInterceptor.setParamName("lang");
        return localeChangeInterceptor;
    }

    @SuppressWarnings("unchecked")
    private static LocalValidatorFactoryBean validator(
            final MessageSource messageSource,
            final UserEmailValidator userEmailValidator,
            final UsernameValidator usernameValidator) {
        final ConstraintValidatorFactory customConstraintFactory =
                new ConstraintValidatorFactory() {
                    @Override
                    public <T extends ConstraintValidator<?, ?>> T getInstance(final Class<T> key) {
                        if (key == UserEmailValidator.class) {
                            return (T) userEmailValidator;
                        } else if (key == UsernameValidator.class) {
                            return (T) usernameValidator;
                        }
                        try {
                            return key.getDeclaredConstructor().newInstance();
                        } catch (final Exception e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public void releaseInstance(final ConstraintValidator<?, ?> instance) {}
                };

        final LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.setValidationMessageSource(messageSource);
        validator.setConstraintValidatorFactory(customConstraintFactory);
        validator.afterPropertiesSet();
        return validator;
    }
}
