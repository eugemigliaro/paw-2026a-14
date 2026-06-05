package ar.edu.itba.paw.webapp.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import ar.edu.itba.paw.models.types.EventType;
import ar.edu.itba.paw.services.MatchParticipationService;
import ar.edu.itba.paw.services.MatchReservationService;
import ar.edu.itba.paw.services.MatchService;
import ar.edu.itba.paw.services.TournamentService;
import ar.edu.itba.paw.webapp.form.SearchForm;
import ar.edu.itba.paw.webapp.utils.AuthenticationUtils;
import ar.edu.itba.paw.webapp.utils.UserUtils;
import java.math.BigDecimal;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.server.ResponseStatusException;

class MatchDashboardControllerTest {

    private MatchDashboardController controller;

    @BeforeEach
    void setUp() {
        final MatchService matchService = Mockito.mock(MatchService.class);
        final MatchParticipationService matchParticipationService =
                Mockito.mock(MatchParticipationService.class);
        final MatchReservationService matchReservationService =
                Mockito.mock(MatchReservationService.class);
        final TournamentService tournamentService = Mockito.mock(TournamentService.class);

        controller =
                new MatchDashboardController(
                        matchService,
                        matchParticipationService,
                        matchReservationService,
                        tournamentService,
                        messageSource());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void showEventsPageWithInvalidBindingReturnsControlledResponse() {
        AuthenticationUtils.authenticateUser(1L);

        final SearchForm searchForm = new SearchForm();
        searchForm.setQ("bad!");
        searchForm.setType(EventType.MATCH);
        searchForm.setMinPrice(BigDecimal.ONE);

        final BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(searchForm, "searchForm");
        bindingResult.rejectValue("q", "Pattern");

        try {
            controller.showEventsPage(
                    UserUtils.getUser(1L), searchForm, bindingResult, Locale.ENGLISH);
            fail("Expected a bad request response");
        } catch (final ResponseStatusException exception) {
            assertEquals(400, exception.getStatus().value());
        }
    }

    private static MessageSource messageSource() {
        final ReloadableResourceBundleMessageSource messageSource =
                new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:i18n/messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);
        return messageSource;
    }
}
