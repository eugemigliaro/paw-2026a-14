package ar.edu.itba.paw.webapp.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.Sport;
import ar.edu.itba.paw.services.MatchService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

class PawUiRouteTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        final InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".jsp");

        final MatchService matchService =
                new MatchService() {
                    @Override
                    public Match createMatch(
                            final Long hostUserId,
                            final String address,
                            final String title,
                            final String description,
                            final Instant startsAt,
                            final Instant endsAt,
                            final int maxPlayers,
                            final BigDecimal pricePerPlayer,
                            final Sport sport,
                            final String visibility,
                            final String status) {
                        return null;
                    }

                    @Override
                    public PaginatedResult<Match> searchPublicMatches(
                            final String query,
                            final String sport,
                            final String time,
                            final String sort,
                            final int page,
                            final int pageSize,
                            final String timezone) {
                        return new PaginatedResult<>(List.of(), 0, 1, 12);
                    }
                };

        mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new FeedController(matchService),
                                new EventController(),
                                new HostController(),
                                new UiController())
                        .setViewResolvers(viewResolver)
                        .build();
    }

    @Test
    void getFeedRouteRendersFeedPage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("feed/index"))
                .andExpect(model().attributeExists("shell"))
                .andExpect(model().attributeExists("feedPage"));
    }

    @Test
    void getEventDetailsRouteRendersEventPage() throws Exception {
        mockMvc.perform(get("/events/sunrise-padel-championship"))
                .andExpect(status().isOk())
                .andExpect(view().name("events/detail"))
                .andExpect(model().attributeExists("shell"))
                .andExpect(model().attributeExists("eventPage"));
    }

    @Test
    void getHostCreateRouteRendersCreatePage() throws Exception {
        mockMvc.perform(get("/host/events/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("host/create-event"))
                .andExpect(model().attributeExists("shell"))
                .andExpect(model().attributeExists("createPage"));
    }

    @Test
    void getComponentsRouteRendersComponentPreviewPage() throws Exception {
        mockMvc.perform(get("/ui/components"))
                .andExpect(status().isOk())
                .andExpect(view().name("ui/components"))
                .andExpect(model().attributeExists("shell"))
                .andExpect(model().attributeExists("componentPage"));
    }

    @Test
    void getUnknownEventReturnsNotFound() throws Exception {
        mockMvc.perform(get("/events/unknown")).andExpect(status().isNotFound());
    }
}
