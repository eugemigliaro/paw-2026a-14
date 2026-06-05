package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.User;

public interface SecurityService {

    boolean isAuthenticated();

    User currentUser();

    boolean isHost(Long matchId);

    boolean hasReviewed(String username);
}
