package ar.edu.itba.paw.webapp.config;

import ar.edu.itba.paw.services.AccountAuthService;
import ar.edu.itba.paw.webapp.security.AccountAuthenticationProvider;
import ar.edu.itba.paw.webapp.security.ContinueFlagLoginEntryPoint;
import ar.edu.itba.paw.webapp.security.LoginFailureHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            final HttpSecurity http,
            final AccountAuthenticationProvider accountAuthenticationProvider,
            final LoginFailureHandler loginFailureHandler)
            throws Exception {
        final HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
        requestCache.setMatchingRequestParameterName("continue");

        http.authenticationProvider(accountAuthenticationProvider)
                .authorizeHttpRequests(
                        authorize ->
                                authorize
                                        .requestMatchers(
                                                new AntPathRequestMatcher("/"),
                                                new AntPathRequestMatcher("/css/**"),
                                                new AntPathRequestMatcher("/js/**"),
                                                new AntPathRequestMatcher("/assets/**"),
                                                new AntPathRequestMatcher(
                                                        "/users/**", HttpMethod.GET.name()),
                                                new AntPathRequestMatcher("/errors/**"),
                                                new AntPathRequestMatcher("/login"),
                                                new AntPathRequestMatcher("/register"),
                                                new AntPathRequestMatcher(
                                                        "/register/resend-verification"),
                                                new AntPathRequestMatcher("/forgot-password"),
                                                new AntPathRequestMatcher("/password-reset/**"),
                                                new AntPathRequestMatcher("/verifications/**"))
                                        .permitAll()
                                        .requestMatchers(
                                                new AntPathRequestMatcher(
                                                        "/matches/**", HttpMethod.GET.name()))
                                        .permitAll()
                                        .requestMatchers(
                                                new AntPathRequestMatcher(
                                                        "/images/**", HttpMethod.GET.name()))
                                        .permitAll()
                                        .requestMatchers(
                                                new AntPathRequestMatcher(
                                                        "/matches/*/reservations",
                                                        HttpMethod.POST.name()),
                                                new AntPathRequestMatcher(
                                                        "/matches/*/join-requests",
                                                        HttpMethod.POST.name()),
                                                new AntPathRequestMatcher(
                                                        "/matches/*/join-requests/cancel",
                                                        HttpMethod.POST.name()))
                                        .hasAnyRole("USER", "ADMIN_MOD")
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
                                        .permitAll())
                .logout(
                        logout ->
                                logout.logoutUrl("/logout")
                                        .logoutSuccessUrl("/login?logout=1")
                                        .permitAll())
                .exceptionHandling(
                        ex -> ex.authenticationEntryPoint(new ContinueFlagLoginEntryPoint()))
                .requestCache(cache -> cache.requestCache(requestCache));

        return http.build();
    }

    @Bean
    public AccountAuthenticationProvider accountAuthenticationProvider(
            final AccountAuthService accountAuthService, final PasswordEncoder passwordEncoder) {
        return new AccountAuthenticationProvider(accountAuthService, passwordEncoder);
    }

    @Bean
    public LoginFailureHandler loginFailureHandler() {
        return new LoginFailureHandler();
    }
}
