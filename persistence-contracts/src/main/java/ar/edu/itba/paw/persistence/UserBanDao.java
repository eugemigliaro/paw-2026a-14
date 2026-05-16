package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.ModerationReport;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserBan;
import java.time.Instant;
import java.util.Optional;

public interface UserBanDao {

    UserBan createBan(ModerationReport moderationReport, Instant bannedUntil);

    Optional<UserBan> findById(Long banId);

    Optional<UserBan> findLatestBanForUser(User user);

    Optional<UserBan> findActiveBanForUser(User user, Instant now);

    void upliftBan(Long banId);
}
