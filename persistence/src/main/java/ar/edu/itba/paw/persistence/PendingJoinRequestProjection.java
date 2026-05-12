package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.PendingJoinRequest;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.types.ParticipantScope;

public record PendingJoinRequestProjection(
        Long matchId,
        Long userId,
        String email,
        String username,
        String name,
        String lastName,
        String phone,
        Long profileImageId,
        String preferredLanguage,
        ParticipantScope scope) {

    public PendingJoinRequest toModel(Match match) {
        final User user =
                new User(
                        userId,
                        email,
                        username,
                        name,
                        lastName,
                        phone,
                        profileImageId,
                        preferredLanguage);
        return new PendingJoinRequest(match, user, scope == ParticipantScope.SERIES);
    }
}
