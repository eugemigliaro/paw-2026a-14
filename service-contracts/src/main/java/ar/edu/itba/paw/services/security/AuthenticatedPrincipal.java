package ar.edu.itba.paw.services.security;

import ar.edu.itba.paw.models.User;

public interface AuthenticatedPrincipal {

    User getAuthenticatedUser();
}
