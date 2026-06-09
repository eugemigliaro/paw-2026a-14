package ar.edu.itba.paw.webapp.config;

import ar.edu.itba.paw.services.AccountAuthService;
import ar.edu.itba.paw.services.ModerationService;
import ar.edu.itba.paw.services.SecurityService;
import ar.edu.itba.paw.webapp.security.AccountAuthenticationProvider;
import ar.edu.itba.paw.webapp.security.BannedAccountAuthorizationFilter;
import ar.edu.itba.paw.webapp.security.ContinueFlagLoginEntryPoint;
import ar.edu.itba.paw.webapp.security.LoginFailureHandler;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static Long longVar(RequestAuthorizationContext ctx, String name) {
        return Long.parseLong(ctx.getVariables().get(name));
    }

    private static String strVar(RequestAuthorizationContext ctx, String name) {
        return ctx.getVariables().get(name);
    }

    private static AuthorizationDecision allow(boolean granted) {
        return new AuthorizationDecision(granted);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            final HttpSecurity http,
            final AccountAuthenticationProvider accountAuthenticationProvider,
            final LoginFailureHandler loginFailureHandler,
            final BannedAccountAuthorizationFilter bannedAccountAuthorizationFilter,
            final SecurityService security)
            throws Exception {

        final HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
        requestCache.setMatchingRequestParameterName("continue");

        http.authenticationProvider(accountAuthenticationProvider)
                .authorizeHttpRequests(
                        authorize ->
                                authorize
                                        .requestMatchers("/", "/errors/**")
                                        .permitAll()
                                        .requestMatchers(
                                                HttpMethod.GET,
                                                "/login",
                                                "/register",
                                                "/forgot-password",
                                                "/password-reset/**")
                                        .anonymous()
                                        .requestMatchers(
                                                HttpMethod.POST,
                                                "/register",
                                                "/register/resend-verification",
                                                "/forgot-password",
                                                "/password-reset/**")
                                        .anonymous()
                                        .requestMatchers("/verifications/**")
                                        .anonymous()
                                        .requestMatchers(
                                                HttpMethod.GET,
                                                "/matches/**",
                                                "/tournaments/**",
                                                "/images/**",
                                                "/users/**")
                                        .permitAll()
                                        .requestMatchers(HttpMethod.POST, "/explore/location")
                                        .permitAll()
                                        .requestMatchers(
                                                HttpMethod.POST,
                                                "/matches/*/reservations",
                                                "/matches/*/reservations/cancel",
                                                "/matches/*/recurring-reservations",
                                                "/matches/*/series-reservations",
                                                "/matches/*/recurring-reservations/cancel",
                                                "/matches/*/series-reservations/cancel",
                                                "/matches/*/join-requests",
                                                "/matches/*/recurring-join-requests",
                                                "/matches/*/series-join-requests",
                                                "/matches/*/join-requests/cancel",
                                                "/matches/*/invites/accept",
                                                "/matches/*/invites/decline")
                                        .hasAnyRole("USER", "ADMIN_MOD")
                                        .requestMatchers(
                                                HttpMethod.POST,
                                                "/tournaments/*/solo-entry",
                                                "/tournaments/*/solo-entry/leave")
                                        .hasAnyRole("USER", "ADMIN_MOD")
                                        .requestMatchers(
                                                HttpMethod.POST, "/users/{username}/reviews")
                                        .access(
                                                (auth, ctx) ->
                                                        allow(
                                                                security.canReviewUser(
                                                                        strVar(ctx, "username"))))
                                        .requestMatchers(
                                                HttpMethod.POST, "/users/{username}/reviews/delete")
                                        .access(
                                                (auth, ctx) ->
                                                        allow(
                                                                security.canDeleteReview(
                                                                        strVar(ctx, "username"))))
                                        .requestMatchers(
                                                "/reports/users/{username}",
                                                "/reports/reviews/{reviewId}",
                                                "/reports/matches/{matchId}")
                                        .access(
                                                (auth, ctx) -> {
                                                    String uri = ctx.getRequest().getRequestURI();
                                                    if (uri.contains("/users/"))
                                                        return allow(
                                                                security.canReportUser(
                                                                        strVar(ctx, "username")));
                                                    if (uri.contains("/reviews/"))
                                                        return allow(
                                                                security.canReportReview(
                                                                        longVar(ctx, "reviewId")));
                                                    return allow(
                                                            security.canReportMatch(
                                                                    longVar(ctx, "matchId")));
                                                })
                                        .requestMatchers(HttpMethod.GET, "/reports/mine/{reportId}")
                                        .access(
                                                (auth, ctx) ->
                                                        allow(
                                                                security.canViewOwnReport(
                                                                        longVar(ctx, "reportId"))))
                                        .requestMatchers(
                                                HttpMethod.POST, "/reports/mine/{reportId}/appeal")
                                        .access(
                                                (auth, ctx) ->
                                                        allow(
                                                                security.canAppealReport(
                                                                        longVar(ctx, "reportId"))))
                                        .requestMatchers("/reports/**")
                                        .hasAnyRole("USER", "ADMIN_MOD")
                                        .requestMatchers(HttpMethod.GET, "/account/ban")
                                        .access((auth, ctx) -> allow(security.canAppealBan()))
                                        .requestMatchers(HttpMethod.POST, "/account/ban/appeal")
                                        .access((auth, ctx) -> allow(security.canAppealBan()))
                                        .requestMatchers("/admin/**", "/moderation/**")
                                        .hasRole("ADMIN_MOD")
                                        .requestMatchers("/host/matches/new")
                                        .hasAnyRole("USER", "ADMIN_MOD")
                                        .requestMatchers(
                                                "/host/matches/{matchId}/edit",
                                                "/host/matches/{matchId}/series/edit")
                                        .access(
                                                (auth, ctx) -> {
                                                    String uri = ctx.getRequest().getRequestURI();
                                                    if (uri.contains("/series/edit"))
                                                        return allow(
                                                                security.canEditMatchSeries(
                                                                        longVar(ctx, "matchId")));
                                                    return allow(
                                                            security.canEditMatch(
                                                                    longVar(ctx, "matchId")));
                                                })
                                        .requestMatchers(
                                                HttpMethod.POST,
                                                "/host/matches/{matchId}/cancel",
                                                "/host/matches/{matchId}/series/cancel")
                                        .access(
                                                (auth, ctx) -> {
                                                    String uri = ctx.getRequest().getRequestURI();
                                                    if (uri.contains("/series/cancel"))
                                                        return allow(
                                                                security.canCancelMatchSeries(
                                                                        longVar(ctx, "matchId")));
                                                    return allow(
                                                            security.canCancelMatch(
                                                                    longVar(ctx, "matchId")));
                                                })
                                        .requestMatchers(
                                                HttpMethod.GET,
                                                "/host/matches/{matchId}/participants")
                                        .access(
                                                (auth, ctx) ->
                                                        allow(
                                                                security.canViewParticipants(
                                                                        longVar(ctx, "matchId"))))
                                        .requestMatchers(
                                                HttpMethod.GET, "/host/matches/{matchId}/requests")
                                        .access(
                                                (auth, ctx) ->
                                                        allow(
                                                                security.canApproveJoinRequests(
                                                                        longVar(ctx, "matchId"))))
                                        .requestMatchers(
                                                HttpMethod.POST,
                                                "/host/matches/{matchId}/requests/{userId}/approve",
                                                "/host/matches/{matchId}/requests/{userId}/reject")
                                        .access(
                                                (auth, ctx) ->
                                                        allow(
                                                                security.canApproveJoinRequests(
                                                                        longVar(ctx, "matchId"))))
                                        .requestMatchers("/host/matches/{matchId}/invites")
                                        .access(
                                                (auth, ctx) ->
                                                        allow(
                                                                security.canInviteParticipants(
                                                                        longVar(ctx, "matchId"))))
                                        .requestMatchers(
                                                HttpMethod.POST,
                                                "/host/matches/{matchId}/participants/{userId}/remove")
                                        .access(
                                                (auth, ctx) ->
                                                        allow(
                                                                security.canManageParticipants(
                                                                        longVar(ctx, "matchId"))))
                                        .requestMatchers(HttpMethod.GET, "/host/tournaments/new")
                                        .hasAnyRole("USER", "ADMIN_MOD")
                                        .requestMatchers(HttpMethod.POST, "/host/tournaments")
                                        .hasAnyRole("USER", "ADMIN_MOD")
                                        .requestMatchers("/host/tournaments/{tournamentId}/edit")
                                        .access(
                                                (auth, ctx) ->
                                                        allow(
                                                                security.canEditTournament(
                                                                        longVar(
                                                                                ctx,
                                                                                "tournamentId"))))
                                        .requestMatchers(
                                                HttpMethod.POST,
                                                "/host/tournaments/{tournamentId}/close-registration",
                                                "/host/tournaments/{tournamentId}/cancel")
                                        .access(
                                                (auth, ctx) ->
                                                        allow(
                                                                security.canCloseRegistration(
                                                                        longVar(
                                                                                ctx,
                                                                                "tournamentId"))))
                                        .requestMatchers(
                                                "/host/tournaments/{tournamentId}/bracket/strategy",
                                                "/host/tournaments/{tournamentId}/bracket/generate",
                                                "/host/tournaments/{tournamentId}/bracket/manual-pairings",
                                                "/host/tournaments/{tournamentId}/bracket/publish",
                                                "/host/tournaments/{tournamentId}/bracket/setup")
                                        .access(
                                                (auth, ctx) ->
                                                        allow(
                                                                security.canManageBracket(
                                                                        longVar(
                                                                                ctx,
                                                                                "tournamentId"))))
                                        .requestMatchers(
                                                HttpMethod.POST,
                                                "/host/tournaments/{tournamentId}/matches/{matchId}/winner")
                                        .access(
                                                (auth, ctx) ->
                                                        allow(
                                                                security.canReportMatchWinner(
                                                                        longVar(
                                                                                ctx,
                                                                                "tournamentId"))))
                                        .anyRequest()
                                        .authenticated())
                .formLogin(
                        form ->
                                form.loginPage("/login")
                                        .loginProcessingUrl("/login")
                                        .usernameParameter("email")
                                        .passwordParameter("password")
                                        .failureHandler(loginFailureHandler))
                .logout(
                        logout ->
                                logout.logoutUrl("/logout")
                                        .logoutSuccessUrl("/login?logout=1")
                                        .permitAll())
                .exceptionHandling(
                        ex -> ex.authenticationEntryPoint(new ContinueFlagLoginEntryPoint()))
                .requestCache(cache -> cache.requestCache(requestCache))
                .addFilterAfter(
                        bannedAccountAuthorizationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers("/css/**", "/js/**", "/assets/**");
    }

    @Bean
    public AccountAuthenticationProvider accountAuthenticationProvider(
            final AccountAuthService accountAuthService,
            final PasswordEncoder passwordEncoder,
            final MessageSource messageSource) {
        return new AccountAuthenticationProvider(
                accountAuthService, passwordEncoder, messageSource);
    }

    @Bean
    public LoginFailureHandler loginFailureHandler() {
        return new LoginFailureHandler();
    }

    @Bean
    public BannedAccountAuthorizationFilter bannedAccountAuthorizationFilter(
            final ModerationService moderationService) {
        return new BannedAccountAuthorizationFilter(moderationService);
    }
}
