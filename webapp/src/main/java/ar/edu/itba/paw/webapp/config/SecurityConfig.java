package ar.edu.itba.paw.webapp.config;

import ar.edu.itba.paw.services.AccountAuthService;
import ar.edu.itba.paw.services.ModerationService;
import ar.edu.itba.paw.services.SecurityService;
import ar.edu.itba.paw.webapp.security.AccountAuthenticationProvider;
import ar.edu.itba.paw.webapp.security.AccountUserDetailsService;
import ar.edu.itba.paw.webapp.security.BannedAccountAuthorizationFilter;
import ar.edu.itba.paw.webapp.security.ContinueFlagLoginEntryPoint;
import ar.edu.itba.paw.webapp.security.LoginFailureHandler;
import ar.edu.itba.paw.webapp.security.RememberMeKey;
import ar.edu.itba.paw.webapp.security.RememberMeLoginSuccessHandler;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices.RememberMeTokenAlgorithm;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String REMEMBER_ME_COOKIE_NAME = "remember-me";
    private static final String REMEMBER_ME_PARAMETER_NAME = "remember-me";
    static final int REMEMBER_ME_TOKEN_VALIDITY_SECONDS = 14 * 24 * 60 * 60;
    private final MvcRequestMatcher.Builder mvc;

    public SecurityConfig(final HandlerMappingIntrospector introspector) {
        this.mvc = new MvcRequestMatcher.Builder(introspector);
    }

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
            final RememberMeLoginSuccessHandler rememberMeLoginSuccessHandler,
            final TokenBasedRememberMeServices rememberMeServices,
            final RememberMeKey rememberMeKey,
            final BannedAccountAuthorizationFilter bannedAccountAuthorizationFilter,
            final SecurityService security)
            throws Exception {

        final HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
        requestCache.setMatchingRequestParameterName("continue");

        http.authenticationProvider(accountAuthenticationProvider)
                .authorizeHttpRequests(
                        authorize ->
                                authorize
                                        .requestMatchers(
                                                mvc.pattern("/"),
                                                mvc.pattern("/errors/**"),
                                                mvc.pattern(
                                                        "/.well-known/appspecific/com.chrome.devtools.json"))
                                        .permitAll()
                                        .requestMatchers(
                                                mvc.pattern(HttpMethod.GET, "/login"),
                                                mvc.pattern(HttpMethod.GET, "/register"),
                                                mvc.pattern(HttpMethod.GET, "/forgot-password"),
                                                mvc.pattern(HttpMethod.GET, "/password-reset/**"))
                                        .anonymous()
                                        .requestMatchers(
                                                mvc.pattern(HttpMethod.POST, "/register"),
                                                mvc.pattern(
                                                        HttpMethod.POST,
                                                        "/register/resend-verification"),
                                                mvc.pattern(HttpMethod.POST, "/forgot-password"),
                                                mvc.pattern(HttpMethod.POST, "/password-reset/**"))
                                        .anonymous()
                                        .requestMatchers(mvc.pattern("/verifications/**"))
                                        .anonymous()
                                        .requestMatchers(
                                                mvc.pattern(HttpMethod.GET, "/matches"),
                                                mvc.pattern(HttpMethod.GET, "/tournaments"),
                                                mvc.pattern(HttpMethod.GET, "/matches/new"),
                                                mvc.pattern(HttpMethod.GET, "/tournaments/new"),
                                                mvc.pattern(HttpMethod.POST, "/matches/new"),
                                                mvc.pattern(HttpMethod.POST, "/tournaments"))
                                        .hasAnyRole("USER", "ADMIN_MOD")
                                        .requestMatchers(
                                                mvc.pattern(HttpMethod.GET, "/matches/**"),
                                                mvc.pattern(HttpMethod.GET, "/tournaments/**"),
                                                mvc.pattern(HttpMethod.GET, "/images/**"),
                                                mvc.pattern(HttpMethod.GET, "/users/**"))
                                        .permitAll()
                                        .requestMatchers(
                                                mvc.pattern(HttpMethod.POST, "/explore/location"))
                                        .permitAll()
                                        .requestMatchers(
                                                mvc.pattern(
                                                        HttpMethod.POST, "/matches/*/reservations"),
                                                mvc.pattern(
                                                        HttpMethod.POST,
                                                        "/matches/*/reservations/cancel"),
                                                mvc.pattern(
                                                        HttpMethod.POST,
                                                        "/matches/*/recurring-reservations"),
                                                mvc.pattern(
                                                        HttpMethod.POST,
                                                        "/matches/*/series-reservations"),
                                                mvc.pattern(
                                                        HttpMethod.POST,
                                                        "/matches/*/recurring-reservations/cancel"),
                                                mvc.pattern(
                                                        HttpMethod.POST,
                                                        "/matches/*/series-reservations/cancel"),
                                                mvc.pattern(
                                                        HttpMethod.POST,
                                                        "/matches/*/join-requests"),
                                                mvc.pattern(
                                                        HttpMethod.POST,
                                                        "/matches/*/recurring-join-requests"),
                                                mvc.pattern(
                                                        HttpMethod.POST,
                                                        "/matches/*/series-join-requests"),
                                                mvc.pattern(
                                                        HttpMethod.POST,
                                                        "/matches/*/join-requests/cancel"),
                                                mvc.pattern(
                                                        HttpMethod.POST,
                                                        "/matches/*/invites/accept"),
                                                mvc.pattern(
                                                        HttpMethod.POST,
                                                        "/matches/*/invites/decline"))
                                        .hasAnyRole("USER", "ADMIN_MOD")
                                        .requestMatchers(
                                                mvc.pattern(
                                                        HttpMethod.POST,
                                                        "/tournaments/*/solo-entry"),
                                                mvc.pattern(
                                                        HttpMethod.POST,
                                                        "/tournaments/*/solo-entry/leave"))
                                        .hasAnyRole("USER", "ADMIN_MOD")
                                        .requestMatchers(
                                                mvc.pattern(
                                                        HttpMethod.POST,
                                                        "/users/{username}/reviews"))
                                        .access(
                                                (auth, ctx) ->
                                                        allow(
                                                                security.canReviewUser(
                                                                        strVar(ctx, "username"))))
                                        .requestMatchers(
                                                mvc.pattern(
                                                        HttpMethod.POST,
                                                        "/users/{username}/reviews/delete"))
                                        .access(
                                                (auth, ctx) ->
                                                        allow(
                                                                security.canDeleteReview(
                                                                        strVar(ctx, "username"))))
                                        .requestMatchers(
                                                mvc.pattern("/reports/users/{username}"),
                                                mvc.pattern("/reports/reviews/{reviewId}"),
                                                mvc.pattern("/reports/matches/{matchId}"))
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
                                        .requestMatchers(
                                                mvc.pattern(
                                                        HttpMethod.GET, "/reports/mine/{reportId}"))
                                        .access(
                                                (auth, ctx) ->
                                                        allow(
                                                                security.canViewOwnReport(
                                                                        longVar(ctx, "reportId"))))
                                        .requestMatchers(
                                                mvc.pattern(
                                                        HttpMethod.POST,
                                                        "/reports/mine/{reportId}/appeal"))
                                        .access(
                                                (auth, ctx) ->
                                                        allow(
                                                                security.canAppealReport(
                                                                        longVar(ctx, "reportId"))))
                                        .requestMatchers(mvc.pattern("/reports/**"))
                                        .hasAnyRole("USER", "ADMIN_MOD")
                                        .requestMatchers(
                                                mvc.pattern(HttpMethod.GET, "/account/ban"))
                                        .access((auth, ctx) -> allow(security.canAppealBan()))
                                        .requestMatchers(
                                                mvc.pattern(HttpMethod.POST, "/account/ban/appeal"))
                                        .access((auth, ctx) -> allow(security.canAppealBan()))
                                        .requestMatchers(
                                                mvc.pattern("/admin/**"),
                                                mvc.pattern("/moderation/**"))
                                        .hasRole("ADMIN_MOD")
                                        .requestMatchers(mvc.pattern("/host/matches/new"))
                                        .hasAnyRole("USER", "ADMIN_MOD")
                                        .requestMatchers(
                                                mvc.pattern("/host/matches/{matchId}/edit"),
                                                mvc.pattern("/host/matches/{matchId}/series/edit"))
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
                                                mvc.pattern(
                                                        HttpMethod.POST,
                                                        "/host/matches/{matchId}/cancel"),
                                                mvc.pattern(
                                                        HttpMethod.POST,
                                                        "/host/matches/{matchId}/series/cancel"))
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
                                                mvc.pattern(
                                                        HttpMethod.GET,
                                                        "/host/matches/{matchId}/participants"))
                                        .access(
                                                (auth, ctx) ->
                                                        allow(
                                                                security.canViewParticipants(
                                                                        longVar(ctx, "matchId"))))
                                        .requestMatchers(
                                                mvc.pattern(
                                                        HttpMethod.GET,
                                                        "/host/matches/{matchId}/requests"))
                                        .access(
                                                (auth, ctx) ->
                                                        allow(
                                                                security.canApproveJoinRequests(
                                                                        longVar(ctx, "matchId"))))
                                        .requestMatchers(
                                                mvc.pattern(
                                                        HttpMethod.POST,
                                                        "/host/matches/{matchId}/requests/{userId}/approve"),
                                                mvc.pattern(
                                                        HttpMethod.POST,
                                                        "/host/matches/{matchId}/requests/{userId}/reject"))
                                        .access(
                                                (auth, ctx) ->
                                                        allow(
                                                                security.canApproveJoinRequests(
                                                                        longVar(ctx, "matchId"))))
                                        .requestMatchers(
                                                mvc.pattern("/host/matches/{matchId}/invites"))
                                        .access(
                                                (auth, ctx) ->
                                                        allow(
                                                                security.canInviteParticipants(
                                                                        longVar(ctx, "matchId"))))
                                        .requestMatchers(
                                                mvc.pattern(
                                                        HttpMethod.POST,
                                                        "/host/matches/{matchId}/participants/{userId}/remove"))
                                        .access(
                                                (auth, ctx) ->
                                                        allow(
                                                                security.canManageParticipants(
                                                                        longVar(ctx, "matchId"))))
                                        .requestMatchers(
                                                mvc.pattern(
                                                        HttpMethod.GET, "/host/tournaments/new"))
                                        .hasAnyRole("USER", "ADMIN_MOD")
                                        .requestMatchers(
                                                mvc.pattern(HttpMethod.POST, "/host/tournaments"))
                                        .hasAnyRole("USER", "ADMIN_MOD")
                                        .requestMatchers(
                                                mvc.pattern(
                                                        "/host/tournaments/{tournamentId}/edit"))
                                        .access(
                                                (auth, ctx) ->
                                                        allow(
                                                                security.canEditTournament(
                                                                        longVar(
                                                                                ctx,
                                                                                "tournamentId"))))
                                        .requestMatchers(
                                                mvc.pattern(
                                                        HttpMethod.POST,
                                                        "/host/tournaments/{tournamentId}/close-registration"),
                                                mvc.pattern(
                                                        HttpMethod.POST,
                                                        "/host/tournaments/{tournamentId}/cancel"))
                                        .access(
                                                (auth, ctx) ->
                                                        allow(
                                                                security.canCloseRegistration(
                                                                        longVar(
                                                                                ctx,
                                                                                "tournamentId"))))
                                        .requestMatchers(
                                                mvc.pattern(
                                                        "/host/tournaments/{tournamentId}/bracket/strategy"),
                                                mvc.pattern(
                                                        "/host/tournaments/{tournamentId}/bracket/generate"),
                                                mvc.pattern(
                                                        "/host/tournaments/{tournamentId}/bracket/manual-pairings"),
                                                mvc.pattern(
                                                        "/host/tournaments/{tournamentId}/bracket/publish"),
                                                mvc.pattern(
                                                        "/host/tournaments/{tournamentId}/bracket/setup"))
                                        .access(
                                                (auth, ctx) ->
                                                        allow(
                                                                security.canManageBracket(
                                                                        longVar(
                                                                                ctx,
                                                                                "tournamentId"))))
                                        .requestMatchers(
                                                mvc.pattern(
                                                        HttpMethod.POST,
                                                        "/host/tournaments/{tournamentId}/matches/{matchId}/winner"))
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
                                        .failureHandler(loginFailureHandler)
                                        .successHandler(rememberMeLoginSuccessHandler))
                .rememberMe(
                        remember ->
                                remember.key(rememberMeKey.value())
                                        .rememberMeServices(rememberMeServices)
                                        .rememberMeParameter(REMEMBER_ME_PARAMETER_NAME))
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
        return web ->
                web.ignoring()
                        .requestMatchers(
                                new AntPathRequestMatcher("/css/**"),
                                new AntPathRequestMatcher("/js/**"),
                                new AntPathRequestMatcher("/assets/**"));
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
    public AccountUserDetailsService accountUserDetailsService(
            final AccountAuthService accountAuthService) {
        return new AccountUserDetailsService(accountAuthService);
    }

    @Bean
    public RememberMeKey rememberMeKey(
            @Value("${security.rememberMe.key:}") final String rememberMeKey) {
        return RememberMeKey.fromConfiguredValue(rememberMeKey);
    }

    @Bean
    public TokenBasedRememberMeServices rememberMeServices(
            final RememberMeKey rememberMeKey,
            final AccountUserDetailsService accountUserDetailsService) {
        final TokenBasedRememberMeServices services =
                new TokenBasedRememberMeServices(
                        rememberMeKey.value(),
                        accountUserDetailsService,
                        RememberMeTokenAlgorithm.SHA256);
        services.setMatchingAlgorithm(RememberMeTokenAlgorithm.SHA256);
        services.setCookieName(REMEMBER_ME_COOKIE_NAME);
        services.setParameter(REMEMBER_ME_PARAMETER_NAME);
        services.setTokenValiditySeconds(REMEMBER_ME_TOKEN_VALIDITY_SECONDS);
        return services;
    }

    @Bean
    public RememberMeLoginSuccessHandler rememberMeLoginSuccessHandler() {
        return new RememberMeLoginSuccessHandler(
                REMEMBER_ME_COOKIE_NAME, REMEMBER_ME_PARAMETER_NAME);
    }

    @Bean
    public BannedAccountAuthorizationFilter bannedAccountAuthorizationFilter(
            final ModerationService moderationService) {
        return new BannedAccountAuthorizationFilter(moderationService);
    }
}
