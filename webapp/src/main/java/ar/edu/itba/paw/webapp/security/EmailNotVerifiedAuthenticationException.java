package ar.edu.itba.paw.webapp.security;

import org.springframework.security.core.AuthenticationException;

public class EmailNotVerifiedAuthenticationException extends AuthenticationException {

    public EmailNotVerifiedAuthenticationException(final String message) {
        super(message);
    }
}
