package ar.edu.itba.paw.webapp.config;

import ar.edu.itba.paw.services.AccountAuthService;
import ar.edu.itba.paw.webapp.security.AccountAuthenticationProvider;
import ar.edu.itba.paw.webapp.security.LoginFailureHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            final HttpSecurity http,
            final AccountAuthenticationProvider accountAuthenticationProvider,
            final LoginFailureHandler loginFailureHandler)
            throws Exception {
        http.authenticationProvider(accountAuthenticationProvider)
                .authorizeRequests(
                        authorize ->
                                authorize
                                        .antMatchers(
                                                "/",
                                                "/css/**",
                                                "/js/**",
                                                "/assets/**",
                                                "/errors/**",
                                                "/login",
                                                "/register",
                                                "/register/resend-verification",
                                                "/forgot-password",
                                                "/password-reset/**",
                                                "/verifications/**")
                                        .permitAll()
                                        .antMatchers(HttpMethod.GET, "/matches/**")
                                        .permitAll()
                                        .antMatchers(HttpMethod.GET, "/images/**")
                                        .permitAll()
                                        .antMatchers(HttpMethod.POST, "/matches/*/reservations")
                                        .hasAnyRole("USER", "ADMIN_MOD")
                                        .antMatchers("/host/**")
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
                                        .permitAll());

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
