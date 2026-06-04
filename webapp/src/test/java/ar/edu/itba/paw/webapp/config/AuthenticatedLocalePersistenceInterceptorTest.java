package ar.edu.itba.paw.webapp.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserLanguages;
import ar.edu.itba.paw.models.types.UserRole;
import ar.edu.itba.paw.services.ImageUpload;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.security.AuthenticatedUserPrincipal;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
    private RecordingUserService userService;

    @BeforeEach
    void setUp() {
        userService = new RecordingUserService();
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

        Assertions.assertEquals(
                List.of(new LanguageUpdate(12L, UserLanguages.SPANISH)),
                userService.languageUpdates);
    }

    @Test
    void authenticatedPreservedLangParameterDoesNotPersistLanguage() throws Exception {
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

        Assertions.assertTrue(userService.languageUpdates.isEmpty());
    }

    @Test
    void unsupportedLangSwitchDoesNotPersistDefaultLanguage() throws Exception {
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

        Assertions.assertTrue(userService.languageUpdates.isEmpty());
    }

    @Test
    void anonymousLangSwitchDoesNotPersistLanguage() throws Exception {
        mockMvc.perform(get("/ping").param("lang", "es")).andExpect(status().isOk());

        Assertions.assertTrue(userService.languageUpdates.isEmpty());
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

    private record LanguageUpdate(long userId, String language) {}

    private static class RecordingUserService implements UserService {

        private final List<LanguageUpdate> languageUpdates = new ArrayList<>();

        @Override
        public User createUser(final String email, final String username) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<User> findByEmail(final String email) {
            return Optional.empty();
        }

        @Override
        public Optional<User> findById(final Long id) {
            return Optional.empty();
        }

        @Override
        public List<User> findByIds(final Collection<Long> ids) {
            return List.of();
        }

        @Override
        public Optional<User> findByUsername(final String username) {
            return Optional.empty();
        }

        @Override
        public User updateProfile(
                final User user,
                final String username,
                final String name,
                final String lastName,
                final String phone,
                final ImageUpload profileImage)
                throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updatePreferredLanguage(final User user, final String preferredLanguage) {
            languageUpdates.add(new LanguageUpdate(user.getId(), preferredLanguage));
        }
    }
}
