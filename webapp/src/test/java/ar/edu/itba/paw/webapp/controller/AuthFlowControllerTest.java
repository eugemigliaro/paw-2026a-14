package ar.edu.itba.paw.webapp.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import ar.edu.itba.paw.models.UserAccount;
import ar.edu.itba.paw.models.UserLanguages;
import ar.edu.itba.paw.models.exceptions.verificationFailure.VerificationFailureExpiredException;
import ar.edu.itba.paw.models.exceptions.verificationFailure.VerificationFailureNotFoundException;
import ar.edu.itba.paw.models.types.UserRole;
import ar.edu.itba.paw.services.AccountAuthService;
import ar.edu.itba.paw.services.PasswordResetPreview;
import ar.edu.itba.paw.services.RegisterAccountRequest;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.services.VerificationConfirmationResult;
import ar.edu.itba.paw.services.VerificationPreview;
import ar.edu.itba.paw.services.VerificationRequestResult;
import ar.edu.itba.paw.webapp.exception.AccessExceptionHandler;
import ar.edu.itba.paw.webapp.exception.PasswordResetExceptionHandler;
import ar.edu.itba.paw.webapp.exception.VerificationExceptionHandler;
import ar.edu.itba.paw.webapp.security.AuthenticatedUserPrincipal;
import ar.edu.itba.paw.webapp.validation.UserEmailValidator;
import ar.edu.itba.paw.webapp.validation.UsernameValidator;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.security.core.context.SecurityContext;
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
    private UserService userService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        final InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".jsp");

        accountAuthService = Mockito.mock(AccountAuthService.class);
        userService = Mockito.mock(UserService.class);
        UserEmailValidator userEmailValidator = new UserEmailValidator(userService);
        UsernameValidator usernameValidator = new UsernameValidator(userService);

        final MessageSource messageSource = messageSource();
        final LocalValidatorFactoryBean validator =
                validator(messageSource, userEmailValidator, usernameValidator);

        mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new AuthController(accountAuthService, messageSource),
                                new PasswordResetController(accountAuthService),
                                new VerificationController(accountAuthService))
                        .setViewResolvers(viewResolver)
                        .setLocaleResolver(localeResolver())
                        .addInterceptors(localeChangeInterceptor())
                        .setValidator(validator)
                        .setControllerAdvice(
                                new AccessExceptionHandler(),
                                new PasswordResetExceptionHandler(),
                                new VerificationExceptionHandler())
                        .build();
    }

    @Test
    void getLoginRouteRendersLoginPage() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(model().attributeDoesNotExist("shell"))
                .andReturn();
    }

    @Test
    void getRegisterRouteRendersRegisterPageWithInactiveExploreNav() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeDoesNotExist("shell"))
                .andReturn();
    }

    @Test
    void getForgotPasswordRouteRendersForgotPasswordPageWithInactiveExploreNav() throws Exception {
        mockMvc.perform(get("/forgot-password"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/forgot-password"))
                .andExpect(model().attributeDoesNotExist("shell"))
                .andReturn();
    }

    @Test
    void postRegisterSuccessRendersCheckEmailPage() throws Exception {
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
                                .param("phone", "+1 555 123 4567")
                                .param("password", "Password123!")
                                .param("confirmPassword", "Password123!"))
                .andExpect(status().isOk())
                .andExpect(view().name("verification/check-email"))
                .andExpect(model().attributeExists("summary"))
                .andExpect(model().attributeExists("expiresAtLabel"))
                .andReturn();
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
    void postRegisterWithNameErrorRerendersRegisterViewWithNameError() throws Exception {
        mockMvc.perform(
                        post("/register")
                                .param("email", "new@test.com")
                                .param("username", "new_user")
                                .param("name", "")
                                .param("lastName", "Rivera")
                                .param("password", "Password123!")
                                .param("confirmPassword", "Password123!"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeHasFieldErrors("registerForm", "name"));
    }

    @Test
    void postRegisterWithLastNameErrorRerendersRegisterViewWithLastNameError() throws Exception {
        mockMvc.perform(
                        post("/register")
                                .param("email", "new@test.com")
                                .param("username", "new_user")
                                .param("name", "Jamie")
                                .param("lastName", "")
                                .param("password", "Password123!")
                                .param("confirmPassword", "Password123!"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeHasFieldErrors("registerForm", "lastName"));
    }

    @Test
    void postRegisterWithPhoneErrorRerendersRegisterViewWithPhoneError() throws Exception {
        mockMvc.perform(
                        post("/register")
                                .param("email", "new@test.com")
                                .param("username", "new_user")
                                .param("name", "Jamie")
                                .param("lastName", "Rivera")
                                .param("phone", "invalid-phone")
                                .param("password", "Password123!")
                                .param("confirmPassword", "Password123!"))
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

        mockMvc.perform(get("/password-reset/reset-token"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/reset-password"))
                .andExpect(model().attributeExists("resetPreview"))
                .andExpect(model().attributeExists("resetPath"))
                .andReturn();
    }

    @Test
    void postPasswordResetSuccessRedirectsToLogin() throws Exception {
        Mockito.when(accountAuthService.resetPassword("reset-token", "NewPassword123!"))
                .thenReturn(new VerificationConfirmationResult(10L));

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
                                "player@test.com", Instant.parse("2026-04-11T18:00:00Z")));

        mockMvc.perform(get("/verifications/account-token"))
                .andExpect(status().isOk())
                .andExpect(view().name("verification/confirm"))
                .andExpect(model().attributeExists("preview"))
                .andReturn();
    }

    @Test
    void postVerificationConfirmAuthenticatesAndRedirectsHome() throws Exception {
        Mockito.when(accountAuthService.confirmVerification("abc123"))
                .thenReturn(
                        new VerificationConfirmationResult(
                                new UserAccount(
                                        9L,
                                        "player@test.com",
                                        "player_account",
                                        "Player",
                                        "Account",
                                        null,
                                        null,
                                        "{bcrypt}hash",
                                        UserRole.USER,
                                        Instant.parse("2026-04-05T00:00:00Z"),
                                        UserLanguages.DEFAULT_LANGUAGE)));

        final MvcResult result =
                mockMvc.perform(post("/verifications/abc123/confirm"))
                        .andExpect(status().is3xxRedirection())
                        .andExpect(redirectedUrl("/"))
                        .andReturn();

        final SecurityContext securityContext =
                (SecurityContext)
                        result.getRequest().getSession().getAttribute("SPRING_SECURITY_CONTEXT");
        Assertions.assertNotNull(securityContext);
        Assertions.assertTrue(securityContext.getAuthentication().isAuthenticated());
        Assertions.assertEquals(
                9L,
                ((AuthenticatedUserPrincipal) securityContext.getAuthentication().getPrincipal())
                        .getUser()
                        .getId());
    }

    @Test
    void getInvalidVerificationRendersNotFoundPage() throws Exception {
        Mockito.when(accountAuthService.getVerificationPreview("invalid"))
                .thenThrow(new VerificationFailureNotFoundException());

        mockMvc.perform(get("/verifications/invalid")).andExpect(status().isNotFound());
    }

    @Test
    void getPasswordResetWithExpiredTokenRendersErrorPage() throws Exception {
        Mockito.when(accountAuthService.getPasswordResetPreview("expired-token"))
                .thenThrow(new VerificationFailureExpiredException());

        mockMvc.perform(get("/password-reset/expired-token"))
                .andExpect(status().isOk())
                .andExpect(view().name("verification/error"))
                .andExpect(model().attribute("backHref", "/forgot-password"))
                .andExpect(model().attribute("messageCode", "verification.message.expired"))
                .andReturn();
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

    private static LocalValidatorFactoryBean validator(
            final MessageSource messageSource,
            final UserEmailValidator userEmailValidator,
            final UsernameValidator usernameValidator) {
        ConstraintValidatorFactory customConstraintFactory =
                new ConstraintValidatorFactory() {
                    @Override
                    public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
                        if (key == UserEmailValidator.class) {
                            return (T) userEmailValidator;
                        } else if (key == UsernameValidator.class) {
                            return (T) usernameValidator;
                        }
                        try {
                            return key.getDeclaredConstructor().newInstance();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public void releaseInstance(ConstraintValidator<?, ?> instance) {}
                }; // TODO: find a better way to inject the custom validator without having to
        // reimplement the whole factory

        final LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.setValidationMessageSource(messageSource);
        validator.setConstraintValidatorFactory(customConstraintFactory);
        validator.afterPropertiesSet();
        return validator;
    }
}
