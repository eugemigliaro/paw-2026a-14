package ar.edu.itba.paw.webapp.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import ar.edu.itba.paw.models.ModerationReport;
import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.types.ReportReason;
import ar.edu.itba.paw.models.types.ReportStatus;
import ar.edu.itba.paw.models.types.ReportTargetType;
import ar.edu.itba.paw.services.ModerationService;
import ar.edu.itba.paw.services.utils.UserUtils;
import ar.edu.itba.paw.webapp.utils.AuthenticationUtils;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class UserModerationReportControllerTest {

    private MockMvc mockMvc;
    private ModerationService moderationService;

    @BeforeEach
    void setUp() {
        moderationService = Mockito.mock(ModerationService.class);
        final MessageSource messageSource = Mockito.mock(MessageSource.class);
        Mockito.when(
                        messageSource.getMessage(
                                ArgumentMatchers.anyString(),
                                ArgumentMatchers.any(),
                                ArgumentMatchers.any(Locale.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new UserModerationReportController(
                                        moderationService, messageSource))
                        .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMyReportsRendersList() throws Exception {
        AuthenticationUtils.authenticateUser(7L);
        Mockito.when(moderationService.findReportsByReporter(7L, List.of(), List.of(), 1, 4))
                .thenReturn(new PaginatedResult<>(List.of(sampleReport()), 1, 1, 4));

        mockMvc.perform(get("/reports/mine"))
                .andExpect(status().isOk())
                .andExpect(view().name("reports/mine/list"));
    }

    @Test
    void getMyReportsAppliesBackendFilters() throws Exception {
        AuthenticationUtils.authenticateUser(7L);
        Mockito.when(
                        moderationService.findReportsByReporter(
                                7L,
                                List.of(ReportTargetType.MATCH, ReportTargetType.REVIEW),
                                List.of(ReportStatus.PENDING, ReportStatus.RESOLVED),
                                1,
                                4))
                .thenReturn(new PaginatedResult<>(List.of(sampleReport()), 1, 1, 4));

        mockMvc.perform(
                        get("/reports/mine")
                                .param("type", "match", "review")
                                .param("status", "pending", "resolved"))
                .andExpect(status().isOk())
                .andExpect(view().name("reports/mine/list"));
    }

    @Test
    void postAppealRedirectsToReportDetail() throws Exception {
        AuthenticationUtils.authenticateUser(7L);

        mockMvc.perform(
                        post("/reports/mine/90/appeal")
                                .param("appealReason", "Please review again"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reports/mine/90"))
                .andExpect(flash().attribute("action", "appealed"));
    }

    @Test
    void getMyReportDetailReturnsNotFoundForOtherUser() throws Exception {
        AuthenticationUtils.authenticateUser(7L);
        final ModerationReport reportFromOtherUser =
                new ModerationReport(
                        90L,
                        UserUtils.getUser(99L),
                        ReportTargetType.MATCH,
                        42L,
                        ReportReason.OTHER,
                        "details",
                        ReportStatus.RESOLVED,
                        null,
                        null,
                        null,
                        null,
                        null,
                        (short) 0,
                        null,
                        null,
                        null,
                        null,
                        Instant.now(),
                        Instant.now());
        Mockito.when(moderationService.findReportById(90L))
                .thenReturn(Optional.of(reportFromOtherUser));

        mockMvc.perform(get("/reports/mine/90")).andExpect(status().isNotFound());
    }

    private static ModerationReport sampleReport() {
        return new ModerationReport(
                90L,
                UserUtils.getUser(7L),
                ReportTargetType.MATCH,
                42L,
                ReportReason.OTHER,
                "details",
                ReportStatus.RESOLVED,
                null,
                null,
                null,
                null,
                null,
                (short) 0,
                null,
                null,
                null,
                null,
                Instant.now(),
                Instant.now());
    }
}
