package ar.edu.itba.paw.webapp.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import ar.edu.itba.paw.models.ModerationReport;
import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.types.ReportReason;
import ar.edu.itba.paw.models.types.ReportStatus;
import ar.edu.itba.paw.models.types.ReportTargetType;
import ar.edu.itba.paw.services.ModerationService;
import ar.edu.itba.paw.services.ModerationTargetSummary;
import ar.edu.itba.paw.webapp.config.converters.StringToReportStatusConverter;
import ar.edu.itba.paw.webapp.config.converters.StringToReportTargetTypeConverter;
import ar.edu.itba.paw.webapp.controller.UserModerationReportController.ReportView;
import ar.edu.itba.paw.webapp.security.annotation.CurrentUserArgumentResolver;
import ar.edu.itba.paw.webapp.utils.AuthenticationUtils;
import ar.edu.itba.paw.webapp.utils.UserUtils;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
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
                        .setConversionService(conversionService())
                        .setCustomArgumentResolvers(new CurrentUserArgumentResolver())
                        .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMyReportsRendersList() throws Exception {
        AuthenticationUtils.authenticateUser(7L);
        Mockito.when(
                        moderationService.findReportsByReporter(
                                UserUtils.getUser(7L), List.of(), List.of(), 1, 4))
                .thenReturn(new PaginatedResult<>(List.of(sampleReport()), 1, 1, 4));
        Mockito.when(moderationService.resolveTarget(ReportTargetType.MATCH, 42L))
                .thenReturn(
                        new ModerationTargetSummary(
                                ReportTargetType.MATCH, 42L, "Friday football", true));

        mockMvc.perform(get("/reports/mine"))
                .andExpect(status().isOk())
                .andExpect(view().name("reports/mine/list"))
                .andExpect(model().attributeExists("reportViews"));
    }

    @Test
    void getMyReportsExposesPerRowTargetHrefsForLinkableTargets() throws Exception {
        AuthenticationUtils.authenticateUser(7L);
        final ModerationReport matchReport = sampleReport(90L, ReportTargetType.MATCH, 42L);
        final ModerationReport userReport = sampleReport(91L, ReportTargetType.USER, 13L);
        final ModerationReport reviewReport = sampleReport(92L, ReportTargetType.REVIEW, 77L);
        final ModerationReport missingUserReport = sampleReport(93L, ReportTargetType.USER, 99L);
        Mockito.when(
                        moderationService.findReportsByReporter(
                                UserUtils.getUser(7L), List.of(), List.of(), 1, 4))
                .thenReturn(
                        new PaginatedResult<>(
                                List.of(matchReport, userReport, reviewReport, missingUserReport),
                                4,
                                1,
                                4));
        Mockito.when(moderationService.resolveTarget(ReportTargetType.MATCH, 42L))
                .thenReturn(
                        new ModerationTargetSummary(
                                ReportTargetType.MATCH, 42L, "Friday football", true));
        Mockito.when(moderationService.resolveTarget(ReportTargetType.USER, 13L))
                .thenReturn(
                        new ModerationTargetSummary(
                                ReportTargetType.USER, 13L, "Player One", "playerOne", true));
        Mockito.when(moderationService.resolveTarget(ReportTargetType.REVIEW, 77L))
                .thenReturn(
                        new ModerationTargetSummary(
                                ReportTargetType.REVIEW, 77L, "reviewerOne", true));
        Mockito.when(moderationService.resolveTarget(ReportTargetType.USER, 99L))
                .thenReturn(new ModerationTargetSummary(ReportTargetType.USER, 99L, null, false));

        final MvcResult result =
                mockMvc.perform(get("/reports/mine"))
                        .andExpect(status().isOk())
                        .andExpect(view().name("reports/mine/list"))
                        .andReturn();

        final List<ReportView> reportViews = reportViewsFrom(result);
        final ReportView matchView = reportViews.get(0);
        final ReportView userView = reportViews.get(1);
        final ReportView reviewView = reportViews.get(2);
        final ReportView missingUserView = reportViews.get(3);
        Assertions.assertEquals(90L, matchView.getReport().getId());
        Assertions.assertEquals("Friday football", matchView.getTargetSummary().getDisplayName());
        Assertions.assertEquals("/matches/42", matchView.getTargetHref());
        Assertions.assertEquals(91L, userView.getReport().getId());
        Assertions.assertEquals("Player One", userView.getTargetSummary().getDisplayName());
        Assertions.assertEquals("playerOne", userView.getTargetSummary().getTargetSlug());
        Assertions.assertEquals("/users/playerOne", userView.getTargetHref());
        Assertions.assertEquals(92L, reviewView.getReport().getId());
        Assertions.assertNull(reviewView.getTargetHref());
        Assertions.assertEquals(93L, missingUserView.getReport().getId());
        Assertions.assertFalse(missingUserView.getTargetSummary().isFound());
        Assertions.assertNull(missingUserView.getTargetHref());
    }

    @Test
    void getMyReportsAppliesBackendFilters() throws Exception {
        AuthenticationUtils.authenticateUser(7L);
        Mockito.when(
                        moderationService.findReportsByReporter(
                                UserUtils.getUser(7L),
                                List.of(ReportTargetType.MATCH, ReportTargetType.REVIEW),
                                List.of(ReportStatus.PENDING, ReportStatus.RESOLVED),
                                1,
                                4))
                .thenReturn(new PaginatedResult<>(List.of(sampleReport()), 1, 1, 4));
        Mockito.when(moderationService.resolveTarget(ReportTargetType.MATCH, 42L))
                .thenReturn(
                        new ModerationTargetSummary(
                                ReportTargetType.MATCH, 42L, "Friday football", true));

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
        Mockito.when(moderationService.findReportByIdForReporter(90L, UserUtils.getUser(7L)))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/reports/mine/90")).andExpect(status().isNotFound());
    }

    @Test
    void getMyReportDetailExposesTargetForLink() throws Exception {
        AuthenticationUtils.authenticateUser(7L);
        Mockito.when(moderationService.findReportById(90L))
                .thenReturn(Optional.of(sampleReport(90L, ReportTargetType.USER, 13L)));
        Mockito.when(moderationService.resolveTarget(ReportTargetType.USER, 13L))
                .thenReturn(
                        new ModerationTargetSummary(
                                ReportTargetType.USER, 13L, "Player One", "playerOne", true));

        final MvcResult result =
                mockMvc.perform(get("/reports/mine/90"))
                        .andExpect(status().isOk())
                        .andExpect(view().name("reports/mine/detail"))
                        .andReturn();

        final ModerationTargetSummary targetSummary =
                (ModerationTargetSummary) result.getModelAndView().getModel().get("targetSummary");
        Assertions.assertEquals(ReportTargetType.USER, targetSummary.getTargetType());
        Assertions.assertEquals(13L, targetSummary.getTargetId());
        Assertions.assertEquals("Player One", targetSummary.getDisplayName());
        Assertions.assertEquals("playerOne", targetSummary.getTargetSlug());
        Assertions.assertTrue(targetSummary.isFound());
        Assertions.assertEquals(
                "/users/playerOne", result.getModelAndView().getModel().get("targetHref"));
    }

    private static ModerationReport sampleReport() {
        return sampleReport(90L, ReportTargetType.MATCH, 42L);
    }

    private static ModerationReport sampleReport(
            final Long reportId, final ReportTargetType targetType, final Long targetId) {
        return new ModerationReport(
                reportId,
                UserUtils.getUser(7L),
                targetType,
                targetId,
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

    @SuppressWarnings("unchecked")
    private static List<ReportView> reportViewsFrom(final MvcResult result) {
        return (List<ReportView>) result.getModelAndView().getModel().get("reportViews");
    }

    private static DefaultFormattingConversionService conversionService() {
        final DefaultFormattingConversionService conversionService =
                new DefaultFormattingConversionService();
        conversionService.addConverter(new StringToReportStatusConverter());
        conversionService.addConverter(new StringToReportTargetTypeConverter());
        return conversionService;
    }
}
