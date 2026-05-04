package ar.edu.itba.paw.webapp.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserLanguages;
import ar.edu.itba.paw.models.UserRole;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.security.AuthenticatedUserPrincipal;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

class AuthenticatedLocalePersistenceInterceptorTest {

    private MockMvc mockMvc;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = Mockito.mock(UserService.class);
        mockMvc =
                MockMvcBuilders.standaloneSetup(new TestController())
                        .setLocaleResolver(localeResolver())
                        .addInterceptors(
                                localeChangeInterceptor(),
                                new AuthenticatedLocalePersistenceInterceptor(userService))
                        .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatedLangSwitchPersistsResolvedLanguage() throws Exception {
        final AtomicLong capturedUserId = new AtomicLong(-1L);
        final AtomicReference<String> capturedLanguage = new AtomicReference<>();
        Mockito.doAnswer(
                        invocation -> {
                            capturedUserId.set(invocation.getArgument(0));
                            capturedLanguage.set(invocation.getArgument(1));
                            return null;
                        })
                .when(userService)
                .updatePreferredLanguage(Mockito.anyLong(), Mockito.anyString());
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                new AuthenticatedUserPrincipal(
                                        new User(
                                                12L,
                                                "player@test.com",
                                                "player",
                                                null,
                                                null,
                                                null,
                                                null,
                                                UserLanguages.ENGLISH),
                                        UserRole.USER),
                                null,
                                java.util.List.of()));

        mockMvc.perform(get("/ping").param("lang", "es").param("persistLang", "true"))
                .andExpect(status().isOk());

        Assertions.assertEquals(12L, capturedUserId.get());
        Assertions.assertEquals(UserLanguages.SPANISH, capturedLanguage.get());
    }

    @Test
    void authenticatedPreservedLangParameterDoesNotPersistLanguage() throws Exception {
        final AtomicLong capturedUserId = new AtomicLong(-1L);
        Mockito.doAnswer(
                        invocation -> {
                            capturedUserId.set(invocation.getArgument(0));
                            return null;
                        })
                .when(userService)
                .updatePreferredLanguage(Mockito.anyLong(), Mockito.anyString());
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                new AuthenticatedUserPrincipal(
                                        new User(
                                                12L,
                                                "player@test.com",
                                                "player",
                                                null,
                                                null,
                                                null,
                                                null,
                                                UserLanguages.ENGLISH),
                                        UserRole.USER),
                                null,
                                java.util.List.of()));

        mockMvc.perform(get("/ping").param("lang", "es")).andExpect(status().isOk());

        Assertions.assertEquals(-1L, capturedUserId.get());
    }

    @Test
    void unsupportedLangSwitchDoesNotPersistDefaultLanguage() throws Exception {
        final AtomicLong capturedUserId = new AtomicLong(-1L);
        Mockito.doAnswer(
                        invocation -> {
                            capturedUserId.set(invocation.getArgument(0));
                            return null;
                        })
                .when(userService)
                .updatePreferredLanguage(Mockito.anyLong(), Mockito.anyString());
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                new AuthenticatedUserPrincipal(
                                        new User(
                                                12L,
                                                "player@test.com",
                                                "player",
                                                null,
                                                null,
                                                null,
                                                null,
                                                UserLanguages.SPANISH),
                                        UserRole.USER),
                                null,
                                java.util.List.of()));

        mockMvc.perform(get("/ping").param("lang", "fr").param("persistLang", "true"))
                .andExpect(status().isOk());

        Assertions.assertEquals(-1L, capturedUserId.get());
    }

    @Test
    void anonymousLangSwitchDoesNotPersistLanguage() throws Exception {
        final AtomicLong capturedUserId = new AtomicLong(-1L);
        Mockito.doAnswer(
                        invocation -> {
                            capturedUserId.set(invocation.getArgument(0));
                            return null;
                        })
                .when(userService)
                .updatePreferredLanguage(Mockito.anyLong(), Mockito.anyString());

        mockMvc.perform(get("/ping").param("lang", "es")).andExpect(status().isOk());

        Assertions.assertEquals(-1L, capturedUserId.get());
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

    @RestController
    private static class TestController {
        @GetMapping("/ping")
        public String ping() {
            return "ok";
        }
    }
}
