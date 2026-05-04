package ar.edu.itba.paw.services;

public interface SecurityService {

    boolean isAuthenticated();

    Long currentUserId();

    boolean isHost(Long matchId);

    boolean hasReviewed(String username);
}
