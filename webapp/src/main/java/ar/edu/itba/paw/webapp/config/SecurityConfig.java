package ar.edu.itba.paw.webapp.config;

import ar.edu.itba.paw.services.AccountAuthService;
import ar.edu.itba.paw.services.ModerationService;
import ar.edu.itba.paw.webapp.security.AccountAuthenticationProvider;
import ar.edu.itba.paw.webapp.security.AccountUserDetailsService;
import ar.edu.itba.paw.webapp.security.BannedAccountAuthorizationFilter;
import ar.edu.itba.paw.webapp.security.ContinueFlagLoginEntryPoint;
import ar.edu.itba.paw.webapp.security.LoginFailureHandler;
import ar.edu.itba.paw.webapp.security.RememberMeKey;
import ar.edu.itba.paw.webapp.security.RememberMeLoginSuccessHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.DefaultHttpSecurityExpressionHandler;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;
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
    private static final String ROLE_USER = "USER";
    private static final String ROLE_ADMIN_MOD = "ADMIN_MOD";
    static final int REMEMBER_ME_TOKEN_VALIDITY_SECONDS = 14 * 24 * 60 * 60;
    private final MvcRequestMatcher.Builder mvc;
    private final DefaultHttpSecurityExpressionHandler expressionHandler;

    public SecurityConfig(
            final HandlerMappingIntrospector introspector,
            final ApplicationContext applicationContext) {
        this.mvc = new MvcRequestMatcher.Builder(introspector);
        this.expressionHandler = new DefaultHttpSecurityExpressionHandler();
        this.expressionHandler.setApplicationContext(applicationContext);
    }

    private AuthorizationManager<RequestAuthorizationContext> check(final String expression) {
        final WebExpressionAuthorizationManager manager =
                new WebExpressionAuthorizationManager(expression);
        manager.setExpressionHandler(expressionHandler);
        return manager;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            final HttpSecurity http,
            final AccountAuthenticationProvider accountAuthenticationProvider,
            final LoginFailureHandler loginFailureHandler,
            final RememberMeLoginSuccessHandler rememberMeLoginSuccessHandler,
            final TokenBasedRememberMeServices rememberMeServices,
            final RememberMeKey rememberMeKey,
            final BannedAccountAuthorizationFilter bannedAccountAuthorizationFilter)
            throws Exception {

        final HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
        requestCache.setMatchingRequestParameterName("continue");

        http.authenticationProvider(accountAuthenticationProvider)
                .authorizeHttpRequests(
                        authorize ->
                                authorize
                                        .requestMatchers(
                                                mvc.pattern(HttpMethod.GET, "/login"),
                                                mvc.pattern(HttpMethod.GET, "/register"),
                                                mvc.pattern(HttpMethod.GET, "/forgot-password"),
                                                mvc.pattern(HttpMethod.GET, "/password-reset/**"),
                                                mvc.pattern(HttpMethod.POST, "/register"),
                                                mvc.pattern(
                                                        HttpMethod.POST,
                                                        "/register/resend-verification"),
                                                mvc.pattern(HttpMethod.POST, "/forgot-password"),
                                                mvc.pattern(HttpMethod.POST, "/password-reset/**"),
                                                mvc.pattern("/verifications/**"))
                                        .anonymous()
                                        .requestMatchers(
                                                mvc.pattern(HttpMethod.GET, "/matches"),
                                                mvc.pattern(HttpMethod.GET, "/tournaments"),
                                                mvc.pattern(HttpMethod.GET, "/matches/new"),
                                                mvc.pattern(HttpMethod.GET, "/tournaments/new"),
                                                mvc.pattern(HttpMethod.POST, "/matches/new"),
                                                mvc.pattern(HttpMethod.POST, "/tournaments"),
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
                                                        "/matches/*/invites/decline"),
                                                mvc.pattern(
                                                        HttpMethod.POST,
                                                        "/tournaments/*/solo-entry"),
                                                mvc.pattern(
                                                        HttpMethod.POST,
                                                        "/tournaments/*/solo-entry/leave"),
                                                mvc.pattern(
                                                        HttpMethod.POST, "/tournaments/*/teams"),
                                                mvc.pattern(
                                                        HttpMethod.POST,
                                                        "/tournaments/*/teams/*/join"),
                                                mvc.pattern(
                                                        HttpMethod.POST,
                                                        "/tournaments/*/teams/leave"),
                                                mvc.pattern("/host/matches/new"),
                                                mvc.pattern(
                                                        HttpMethod.GET, "/host/tournaments/new"),
                                                mvc.pattern(HttpMethod.POST, "/host/tournaments"))
                                        .hasAnyRole(ROLE_USER, ROLE_ADMIN_MOD)
                                        .requestMatchers(
                                                mvc.pattern("/"),
                                                mvc.pattern("/errors/**"),
                                                mvc.pattern(
                                                        "/.well-known/appspecific/com.chrome.devtools.json"),
                                                mvc.pattern(HttpMethod.GET, "/matches/**"),
                                                mvc.pattern(HttpMethod.GET, "/tournaments/**"),
                                                mvc.pattern(HttpMethod.GET, "/images/**"),
                                                mvc.pattern(HttpMethod.GET, "/users/**"),
                                                mvc.pattern(HttpMethod.POST, "/explore/location"))
                                        .permitAll()
                                        .requestMatchers(
                                                mvc.pattern(
                                                        HttpMethod.POST,
                                                        "/users/{username}/reviews"))
                                        .access(check("@securityService.canReviewUser(#username)"))
                                        .requestMatchers(
                                                mvc.pattern(
                                                        HttpMethod.POST,
                                                        "/users/{username}/reviews/delete"))
                                        .access(
                                                check(
                                                        "@securityService.canDeleteReview(#username)"))
                                        .requestMatchers(mvc.pattern("/reports/users/{username}"))
                                        .access(check("@securityService.canReportUser(#username)"))
                                        .requestMatchers(mvc.pattern("/reports/reviews/{reviewId}"))
                                        .access(
                                                check(
                                                        "@securityService.canReportReview(#reviewId)"))
                                        .requestMatchers(mvc.pattern("/reports/matches/{matchId}"))
                                        .access(check("@securityService.canReportMatch(#matchId)"))
                                        .requestMatchers(
                                                mvc.pattern(
                                                        HttpMethod.GET, "/reports/mine/{reportId}"))
                                        .access(
                                                check(
                                                        "@securityService.canViewOwnReport(#reportId)"))
                                        .requestMatchers(
                                                mvc.pattern(
                                                        HttpMethod.POST,
                                                        "/reports/mine/{reportId}/appeal"))
                                        .access(
                                                check(
                                                        "@securityService.canAppealReport(#reportId)"))
                                        .requestMatchers(mvc.pattern("/reports/**"))
                                        .hasAnyRole(ROLE_USER, ROLE_ADMIN_MOD)
                                        .requestMatchers(
                                                mvc.pattern(HttpMethod.GET, "/account/ban"),
                                                mvc.pattern(HttpMethod.POST, "/account/ban/appeal"))
                                        .access(check("@securityService.canAppealBan()"))
                                        .requestMatchers(
                                                mvc.pattern("/admin/**"),
                                                mvc.pattern("/moderation/**"))
                                        .hasRole(ROLE_ADMIN_MOD)
                                        .requestMatchers(
                                                mvc.pattern("/host/matches/{matchId}/edit"))
                                        .access(check("@securityService.canEditMatch(#matchId)"))
                                        .requestMatchers(
                                                mvc.pattern("/host/matches/{matchId}/series/edit"))
                                        .access(
                                                check(
                                                        "@securityService.canEditMatchSeries(#matchId)"))
                                        .requestMatchers(
                                                mvc.pattern(
                                                        HttpMethod.POST,
                                                        "/host/matches/{matchId}/cancel"))
                                        .access(check("@securityService.canCancelMatch(#matchId)"))
                                        .requestMatchers(
                                                mvc.pattern(
                                                        HttpMethod.POST,
                                                        "/host/matches/{matchId}/series/cancel"))
                                        .access(
                                                check(
                                                        "@securityService.canCancelMatchSeries(#matchId)"))
                                        .requestMatchers(
                                                mvc.pattern(
                                                        HttpMethod.GET,
                                                        "/host/matches/{matchId}/participants"))
                                        .access(
                                                check(
                                                        "@securityService.canViewParticipants(#matchId)"))
                                        .requestMatchers(
                                                mvc.pattern(
                                                        HttpMethod.GET,
                                                        "/host/matches/{matchId}/requests"))
                                        .access(
                                                check(
                                                        "@securityService.canApproveJoinRequests(#matchId)"))
                                        .requestMatchers(
                                                mvc.pattern(
                                                        HttpMethod.POST,
                                                        "/host/matches/{matchId}/requests/{userId}/approve"),
                                                mvc.pattern(
                                                        HttpMethod.POST,
                                                        "/host/matches/{matchId}/requests/{userId}/reject"))
                                        .access(
                                                check(
                                                        "@securityService.canApproveJoinRequests(#matchId)"))
                                        .requestMatchers(
                                                mvc.pattern("/host/matches/{matchId}/invites"))
                                        .access(
                                                check(
                                                        "@securityService.canInviteParticipants(#matchId)"))
                                        .requestMatchers(
                                                mvc.pattern(
                                                        HttpMethod.POST,
                                                        "/host/matches/{matchId}/participants/{userId}/remove"))
                                        .access(
                                                check(
                                                        "@securityService.canManageParticipants(#matchId)"))
                                        .requestMatchers(
                                                mvc.pattern(
                                                        "/host/tournaments/{tournamentId}/edit"))
                                        .access(
                                                check(
                                                        "@securityService.canEditTournament(#tournamentId)"))
                                        .requestMatchers(
                                                mvc.pattern(
                                                        HttpMethod.POST,
                                                        "/host/tournaments/{tournamentId}/close-registration"),
                                                mvc.pattern(
                                                        HttpMethod.POST,
                                                        "/host/tournaments/{tournamentId}/cancel"))
                                        .access(
                                                check(
                                                        "@securityService.canCloseRegistration(#tournamentId)"))
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
                                                check(
                                                        "@securityService.canManageBracket(#tournamentId)"))
                                        .requestMatchers(
                                                mvc.pattern(
                                                        HttpMethod.POST,
                                                        "/host/tournaments/{tournamentId}/matches/{matchId}/winner"))
                                        .access(
                                                check(
                                                        "@securityService.canReportMatchWinner(#tournamentId)"))
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
        return new AccountAuthenticationProvider(accountAuthService, passwordEncoder);
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
