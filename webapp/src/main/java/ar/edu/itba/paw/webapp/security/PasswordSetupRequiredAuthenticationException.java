package ar.edu.itba.paw.webapp.security;

import org.springframework.security.core.AuthenticationException;

public class PasswordSetupRequiredAuthenticationException extends AuthenticationException {

    public PasswordSetupRequiredAuthenticationException() {
        super("passwordSetup");
    }
}
