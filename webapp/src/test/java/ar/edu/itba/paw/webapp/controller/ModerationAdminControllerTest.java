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
import ar.edu.itba.paw.models.UserAccount;
import ar.edu.itba.paw.models.UserLanguages;
import ar.edu.itba.paw.models.types.AppealDecision;
import ar.edu.itba.paw.models.types.ReportReason;
import ar.edu.itba.paw.models.types.ReportResolution;
import ar.edu.itba.paw.models.types.ReportStatus;
import ar.edu.itba.paw.models.types.ReportTargetType;
import ar.edu.itba.paw.models.types.UserRole;
import ar.edu.itba.paw.services.ModerationService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.security.AuthenticatedUserPrincipal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

class ModerationAdminControllerTest {

    private MockMvc mockMvc;
    private ModerationService moderationService;
    private UserService userService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        moderationService = Mockito.mock(ModerationService.class);
        userService = Mockito.mock(UserService.class);

        final InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".jsp");

        mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new ModerationAdminController(
                                        moderationService, messageSource(), userService))
                        .setViewResolvers(viewResolver)
                        .setLocaleResolver(localeResolver())
                        .addInterceptors(localeChangeInterceptor())
                        .build();
    }

    @Test
    void getReportsRendersAdminQueue() throws Exception {
        Mockito.when(moderationService.findReports(List.of(), List.of(), 1, 4))
                .thenReturn(new PaginatedResult<>(List.of(sampleAppealedReport()), 1, 1, 4));

        mockMvc.perform(get("/admin/reports").locale(Locale.ENGLISH))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/reports/list"))
                .andExpect(model().attributeExists("reports"))
                .andExpect(model().attributeExists("emptyMessage"));
    }

    @Test
    void postFinalizeAppealRedirectsToQueue() throws Exception {
        Mockito.when(
                        moderationService.finalizeReportAppeal(
                                Mockito.eq(17L),
                                Mockito.eq(99L),
                                Mockito.eq(AppealDecision.UPHELD)))
                .thenReturn(sampleAppealedReport());

        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                new AuthenticatedUserPrincipal(
                                        new UserAccount(
                                                99L,
                                                "admin@test.com",
                                                "admin",
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                UserRole.ADMIN_MOD,
                                                Instant.parse("2026-04-11T18:00:00Z"),
                                                UserLanguages.DEFAULT_LANGUAGE)),
                                "ignored",
                                List.of(new SimpleGrantedAuthority("ROLE_ADMIN_MOD"))));

        mockMvc.perform(post("/admin/reports/17/finalize-appeal").param("appealDecision", "upheld"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/reports"))
                .andExpect(flash().attribute("action", "appeal_upheld"));
    }

    @Test
    void getReportDetailRendersDetailPage() throws Exception {
        Mockito.when(moderationService.findReportById(17L))
                .thenReturn(Optional.of(sampleAppealedReport()));

        mockMvc.perform(get("/admin/reports/17").locale(Locale.ENGLISH))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/reports/detail"))
                .andExpect(model().attributeExists("report"));
    }

    @Test
    void getReportDetailRendersPendingReportWithoutReviewer() throws Exception {
        Mockito.when(moderationService.findReportById(3L))
                .thenReturn(Optional.of(samplePendingReport()));
        Mockito.when(userService.findById(Mockito.isNull()))
                .thenThrow(new IllegalArgumentException("id to load is required for loading"));

        mockMvc.perform(get("/admin/reports/3").locale(Locale.ENGLISH))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/reports/detail"))
                .andExpect(model().attribute("reviewerUsername", ""));
    }

    @Test
    void getReportDetailReturnsNotFoundWhenMissing() throws Exception {
        Mockito.when(moderationService.findReportById(17L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/admin/reports/17").locale(Locale.ENGLISH))
                .andExpect(status().isNotFound());
    }

    private static ModerationReport sampleAppealedReport() {
        return new ModerationReport(
                17L,
                7L,
                ReportTargetType.USER,
                44L,
                ReportReason.HARASSMENT,
                "Harassing messages",
                ReportStatus.APPEALED,
                ReportResolution.DISMISSED,
                "Original warning",
                99L,
                Instant.parse("2026-04-12T10:00:00Z"),
                "I disagree with this decision",
                (short) 1,
                Instant.parse("2026-04-13T10:00:00Z"),
                null,
                null,
                null,
                Instant.parse("2026-04-12T09:00:00Z"),
                Instant.parse("2026-04-13T10:00:00Z"));
    }

    private static ModerationReport samplePendingReport() {
        return new ModerationReport(
                3L,
                7L,
                ReportTargetType.USER,
                44L,
                ReportReason.HARASSMENT,
                "Harassing messages",
                ReportStatus.PENDING,
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
                Instant.parse("2026-04-12T09:00:00Z"),
                Instant.parse("2026-04-12T09:00:00Z"));
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
}
