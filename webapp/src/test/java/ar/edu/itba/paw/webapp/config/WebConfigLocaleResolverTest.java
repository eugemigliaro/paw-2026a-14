package ar.edu.itba.paw.webapp.config;

import ar.edu.itba.paw.services.UserService;
import java.util.Locale;
import javax.servlet.http.Cookie;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.LocaleResolver;

class WebConfigLocaleResolverTest {

    @Test
    void localeResolverPersistsLocaleInCookieAcrossRequests() {
        final LocaleResolver localeResolver =
                new WebConfig(
                                userServiceProvider(),
                                "jdbc:postgresql://localhost/paw",
                                "paw",
                                "paw")
                        .localeResolver();
        final MockHttpServletResponse firstResponse = new MockHttpServletResponse();

        localeResolver.setLocale(
                new MockHttpServletRequest(), firstResponse, Locale.forLanguageTag("es"));
        final Cookie localeCookie = firstResponse.getCookie("paw_locale");

        final MockHttpServletRequest nextRequest = new MockHttpServletRequest();
        nextRequest.setCookies(localeCookie);

        Assertions.assertNotNull(localeCookie);
        Assertions.assertEquals(
                Locale.forLanguageTag("es"), localeResolver.resolveLocale(nextRequest));
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<UserService> userServiceProvider() {
        return Mockito.mock(ObjectProvider.class);
    }
}
