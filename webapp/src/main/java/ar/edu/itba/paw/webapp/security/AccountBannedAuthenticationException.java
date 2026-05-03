package ar.edu.itba.paw.webapp.security;

import org.springframework.security.core.AuthenticationException;

public class AccountBannedAuthenticationException extends AuthenticationException {

    public AccountBannedAuthenticationException(final String message) {
        super(message);
    }
}
