package ar.edu.itba.paw.webapp.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import ar.edu.itba.paw.services.AccountAuthService;
import ar.edu.itba.paw.services.PasswordResetPreview;
import ar.edu.itba.paw.services.RegisterAccountRequest;
import ar.edu.itba.paw.services.VerificationConfirmationResult;
import ar.edu.itba.paw.services.VerificationFailureException;
import ar.edu.itba.paw.services.VerificationFailureReason;
import ar.edu.itba.paw.services.VerificationPreview;
import ar.edu.itba.paw.services.VerificationRequestResult;
import ar.edu.itba.paw.services.exceptions.AccountRegistrationException;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.ShellViewModel;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

class AuthFlowControllerTest {

    private MockMvc mockMvc;
    private AccountAuthService accountAuthService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        final InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".jsp");
        final MessageSource messageSource = messageSource();
        final LocalValidatorFactoryBean validator = validator(messageSource);

        accountAuthService = Mockito.mock(AccountAuthService.class);

        mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new AuthController(accountAuthService, messageSource),
                                new PasswordResetController(accountAuthService, messageSource),
                                new VerificationController(accountAuthService, messageSource))
                        .setViewResolvers(viewResolver)
                        .setLocaleResolver(localeResolver())
                        .addInterceptors(localeChangeInterceptor())
                        .setValidator(validator)
                        .build();
    }

    @Test
    void getLoginRouteRendersLoginPage() throws Exception {
        final MvcResult result =
                mockMvc.perform(get("/login"))
                        .andExpect(status().isOk())
                        .andExpect(view().name("auth/login"))
                        .andExpect(model().attributeExists("shell"))
                        .andReturn();

        assertExploreIsNotActive(result);
    }

    @Test
    void getRegisterRouteRendersRegisterPageWithInactiveExploreNav() throws Exception {
        final MvcResult result =
                mockMvc.perform(get("/register"))
                        .andExpect(status().isOk())
                        .andExpect(view().name("auth/register"))
                        .andExpect(model().attributeExists("shell"))
                        .andReturn();

        assertExploreIsNotActive(result);
    }

    @Test
    void getForgotPasswordRouteRendersForgotPasswordPageWithInactiveExploreNav() throws Exception {
        final MvcResult result =
                mockMvc.perform(get("/forgot-password"))
                        .andExpect(status().isOk())
                        .andExpect(view().name("auth/forgot-password"))
                        .andExpect(model().attributeExists("shell"))
                        .andReturn();

        assertExploreIsNotActive(result);
    }

    @Test
    void postRegisterSuccessRendersCheckEmailPage() throws Exception {
        Mockito.when(
                        accountAuthService.register(
                                ArgumentMatchers.any(RegisterAccountRequest.class)))
                .thenReturn(
                        new VerificationRequestResult(
                                "new@test.com", Instant.parse("2026-04-11T18:00:00Z")));

        final MvcResult result =
                mockMvc.perform(
                                post("/register")
                                        .param("email", "new@test.com")
                                        .param("username", "new_user")
                                        .param("name", "Jamie")
                                        .param("lastName", "Rivera")
                                        .param("phone", "+1 555 123 4567")
                                        .param("password", "Password123!")
                                        .param("confirmPassword", "Password123!"))
                        .andExpect(status().isOk())
                        .andExpect(view().name("verification/check-email"))
                        .andExpect(model().attributeExists("summary"))
                        .andExpect(model().attributeExists("expiresAtLabel"))
                        .andReturn();

        assertExploreIsNotActive(result);
    }

    @Test
    void postRegisterWithMismatchedPasswordsRerendersRegisterView() throws Exception {
        mockMvc.perform(
                        post("/register")
                                .param("email", "new@test.com")
                                .param("username", "new_user")
                                .param("name", "Jamie")
                                .param("lastName", "Rivera")
                                .param("phone", "+1 555 123 4567")
                                .param("password", "Password123!")
                                .param("confirmPassword", "Password1234!"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeHasFieldErrors("registerForm", "confirmPassword"));
    }

    @Test
    void postRegisterWithoutPhoneStillRendersCheckEmailPage() throws Exception {
        Mockito.when(
                        accountAuthService.register(
                                ArgumentMatchers.any(RegisterAccountRequest.class)))
                .thenReturn(
                        new VerificationRequestResult(
                                "new@test.com", Instant.parse("2026-04-11T18:00:00Z")));

        mockMvc.perform(
                        post("/register")
                                .param("email", "new@test.com")
                                .param("username", "new_user")
                                .param("name", "Jamie")
                                .param("lastName", "Rivera")
                                .param("phone", "")
                                .param("password", "Password123!")
                                .param("confirmPassword", "Password123!"))
                .andExpect(status().isOk())
                .andExpect(view().name("verification/check-email"));
    }

    @Test
    void postRegisterWithServiceNameErrorRerendersRegisterViewWithNameError() throws Exception {
        Mockito.when(
                        accountAuthService.register(
                                ArgumentMatchers.any(RegisterAccountRequest.class)))
                .thenThrow(new AccountRegistrationException("name_invalid", "Invalid name"));

        mockMvc.perform(validRegisterRequest())
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeHasFieldErrors("registerForm", "name"));
    }

    @Test
    void postRegisterWithServiceLastNameErrorRerendersRegisterViewWithLastNameError()
            throws Exception {
        Mockito.when(
                        accountAuthService.register(
                                ArgumentMatchers.any(RegisterAccountRequest.class)))
                .thenThrow(
                        new AccountRegistrationException("lastName_invalid", "Invalid last name"));

        mockMvc.perform(validRegisterRequest())
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeHasFieldErrors("registerForm", "lastName"));
    }

    @Test
    void postRegisterWithServicePhoneErrorRerendersRegisterViewWithPhoneError() throws Exception {
        Mockito.when(
                        accountAuthService.register(
                                ArgumentMatchers.any(RegisterAccountRequest.class)))
                .thenThrow(new AccountRegistrationException("phone_invalid", "Invalid phone"));

        mockMvc.perform(validRegisterRequest())
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeHasFieldErrors("registerForm", "phone"));
    }

    @Test
    void postForgotPasswordRendersGenericCheckEmailPage() throws Exception {
        Mockito.when(accountAuthService.requestPasswordReset("known@test.com"))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/forgot-password").param("email", "known@test.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("verification/check-email"))
                .andExpect(model().attributeExists("summary"));
    }

    @Test
    void getPasswordResetRouteRendersResetPasswordPage() throws Exception {
        Mockito.when(accountAuthService.getPasswordResetPreview("reset-token"))
                .thenReturn(
                        new PasswordResetPreview(
                                "player@test.com", Instant.parse("2026-04-11T18:00:00Z")));

        final MvcResult result =
                mockMvc.perform(get("/password-reset/reset-token"))
                        .andExpect(status().isOk())
                        .andExpect(view().name("auth/reset-password"))
                        .andExpect(model().attributeExists("resetPreview"))
                        .andExpect(model().attributeExists("resetPath"))
                        .andReturn();

        assertExploreIsNotActive(result);
    }

    @Test
    void postPasswordResetSuccessRedirectsToLogin() throws Exception {
        Mockito.when(accountAuthService.resetPassword("reset-token", "NewPassword123!"))
                .thenReturn(
                        new VerificationConfirmationResult(
                                10L, "/login?reset=1", "Password reset"));

        mockMvc.perform(
                        post("/password-reset/reset-token")
                                .param("password", "NewPassword123!")
                                .param("confirmPassword", "NewPassword123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?reset=1"));
    }

    @Test
    void getAccountVerificationPreviewRendersConfirmPage() throws Exception {
        Mockito.when(accountAuthService.getVerificationPreview("account-token"))
                .thenReturn(
                        new VerificationPreview(
                                "Verify your account",
                                "Confirm your email address.",
                                "player@test.com",
                                Instant.parse("2026-04-11T18:00:00Z"),
                                "Verify account",
                                "/login?verified=1",
                                List.of()));

        final MvcResult result =
                mockMvc.perform(get("/verifications/account-token"))
                        .andExpect(status().isOk())
                        .andExpect(view().name("verification/confirm"))
                        .andExpect(model().attributeExists("preview"))
                        .andReturn();

        assertExploreIsNotActive(result);
    }

    @Test
    void getPasswordResetWithExpiredTokenRendersErrorPage() throws Exception {
        Mockito.when(accountAuthService.getPasswordResetPreview("expired-token"))
                .thenThrow(
                        new VerificationFailureException(
                                VerificationFailureReason.EXPIRED, "Expired token"));

        final MvcResult result =
                mockMvc.perform(get("/password-reset/expired-token"))
                        .andExpect(status().isOk())
                        .andExpect(view().name("verification/error"))
                        .andExpect(model().attribute("backHref", "/forgot-password"))
                        .andReturn();

        assertExploreIsNotActive(result);
    }

    private static void assertExploreIsNotActive(final MvcResult result) {
        final ShellViewModel shell =
                (ShellViewModel) result.getModelAndView().getModel().get("shell");

        Assertions.assertFalse(shell.getPrimaryNav().isEmpty());
        Assertions.assertFalse(shell.getPrimaryNav().get(0).isActive());
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
            validRegisterRequest() {
        return post("/register")
                .param("email", "new@test.com")
                .param("username", "new_user")
                .param("name", "Jamie")
                .param("lastName", "Rivera")
                .param("phone", "+1 555 123 4567")
                .param("password", "Password123!")
                .param("confirmPassword", "Password123!");
    }

    private static MessageSource messageSource() {
        final ReloadableResourceBundleMessageSource messageSource =
                new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:i18n/messages");
        messageSource.setDefaultEncoding("UTF-8");
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

    private static LocalValidatorFactoryBean validator(final MessageSource messageSource) {
        final LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.setValidationMessageSource(messageSource);
        validator.afterPropertiesSet();
        return validator;
    }
}
