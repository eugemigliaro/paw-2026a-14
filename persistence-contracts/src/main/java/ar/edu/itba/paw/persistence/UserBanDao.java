package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.ModerationReport;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserBan;
import java.time.Instant;
import java.util.Optional;

public interface UserBanDao {

    UserBan createBan(ModerationReport moderationReport, Instant bannedUntil);

    Optional<UserBan> findById(Long banId);

    default Optional<UserBan> findLatestBanForUser(User user) {
        if (user == null) {
            return Optional.empty();
        }
        return findLatestBanForUser(user.getId());
    }
    ;

    Optional<UserBan> findLatestBanForUser(Long userId);

    default Optional<UserBan> findActiveBanForUser(User user, Instant now) {
        if (user == null) {
            return Optional.empty();
        }
        return findActiveBanForUser(user.getId(), now);
    }

    Optional<UserBan> findActiveBanForUser(Long userId, Instant now);

    void upliftBan(Long banId);
}
