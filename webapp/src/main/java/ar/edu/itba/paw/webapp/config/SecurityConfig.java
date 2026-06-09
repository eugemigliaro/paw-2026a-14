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
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices.RememberMeTokenAlgorithm;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    static final String REMEMBER_ME_COOKIE_NAME = "remember-me";
    static final String REMEMBER_ME_PARAMETER_NAME = "remember-me";
    static final int REMEMBER_ME_TOKEN_VALIDITY_SECONDS = 14 * 24 * 60 * 60;

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
                                                new AntPathRequestMatcher("/"),
                                                new AntPathRequestMatcher("/.well-known/**"),
                                                new AntPathRequestMatcher("/errors/**"))
                                        .permitAll()
                                        .requestMatchers(
                                                new AntPathRequestMatcher(
                                                        "/login", HttpMethod.GET.name()),
                                                new AntPathRequestMatcher(
                                                        "/register", HttpMethod.GET.name()),
                                                new AntPathRequestMatcher(
                                                        "/register", HttpMethod.POST.name()),
                                                new AntPathRequestMatcher(
                                                        "/register/resend-verification"),
                                                new AntPathRequestMatcher("/forgot-password"),
                                                new AntPathRequestMatcher("/password-reset/**"),
                                                new AntPathRequestMatcher("/verifications/**"))
                                        .anonymous()
                                        .requestMatchers(
                                                new AntPathRequestMatcher(
                                                        "/matches/**", HttpMethod.GET.name()),
                                                new AntPathRequestMatcher(
                                                        "/tournaments/**", HttpMethod.GET.name()),
                                                new AntPathRequestMatcher(
                                                        "/images/**", HttpMethod.GET.name()),
                                                new AntPathRequestMatcher(
                                                        "/users/**", HttpMethod.GET.name()))
                                        .permitAll()
                                        .requestMatchers(
                                                new AntPathRequestMatcher(
                                                        "/explore/location",
                                                        HttpMethod.POST.name()))
                                        .permitAll()
                                        .requestMatchers(
                                                new AntPathRequestMatcher(
                                                        "/matches/*/reservations",
                                                        HttpMethod.POST.name()),
                                                new AntPathRequestMatcher(
                                                        "/matches/*/reservations/cancel",
                                                        HttpMethod.POST.name()),
                                                new AntPathRequestMatcher(
                                                        "/matches/*/recurring-reservations",
                                                        HttpMethod.POST.name()),
                                                new AntPathRequestMatcher(
                                                        "/matches/*/series-reservations",
                                                        HttpMethod.POST.name()),
                                                new AntPathRequestMatcher(
                                                        "/matches/*/recurring-reservations/cancel",
                                                        HttpMethod.POST.name()),
                                                new AntPathRequestMatcher(
                                                        "/matches/*/series-reservations/cancel",
                                                        HttpMethod.POST.name()),
                                                new AntPathRequestMatcher(
                                                        "/matches/*/join-requests",
                                                        HttpMethod.POST.name()),
                                                new AntPathRequestMatcher(
                                                        "/matches/*/recurring-join-requests",
                                                        HttpMethod.POST.name()),
                                                new AntPathRequestMatcher(
                                                        "/matches/*/series-join-requests",
                                                        HttpMethod.POST.name()),
                                                new AntPathRequestMatcher(
                                                        "/matches/*/join-requests/cancel",
                                                        HttpMethod.POST.name()),
                                                new AntPathRequestMatcher(
                                                        "/matches/*/invites/accept",
                                                        HttpMethod.POST.name()),
                                                new AntPathRequestMatcher(
                                                        "/matches/*/invites/decline",
                                                        HttpMethod.POST.name()),
                                                new AntPathRequestMatcher(
                                                        "/users/*/reviews", HttpMethod.POST.name()),
                                                new AntPathRequestMatcher(
                                                        "/users/*/reviews/delete",
                                                        HttpMethod.POST.name()),
                                                new AntPathRequestMatcher(
                                                        "/tournaments/*/solo-entry",
                                                        HttpMethod.POST.name()),
                                                new AntPathRequestMatcher(
                                                        "/tournaments/*/solo-entry/leave",
                                                        HttpMethod.POST.name()))
                                        .hasAnyRole("USER", "ADMIN_MOD")
                                        .requestMatchers(new AntPathRequestMatcher("/reports/**"))
                                        .hasAnyRole("USER", "ADMIN_MOD")
                                        .requestMatchers(new AntPathRequestMatcher("/admin/**"))
                                        .hasRole("ADMIN_MOD")
                                        .requestMatchers(
                                                new AntPathRequestMatcher("/moderation/**"))
                                        .hasRole("ADMIN_MOD")
                                        .requestMatchers(new AntPathRequestMatcher("/host/**"))
                                        .hasAnyRole("USER", "ADMIN_MOD")
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
