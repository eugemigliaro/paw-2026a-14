package ar.edu.itba.paw.webapp.utils;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.MatchSeries;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.types.EventJoinPolicy;
import ar.edu.itba.paw.models.types.EventStatus;
import ar.edu.itba.paw.models.types.EventVisibility;
import ar.edu.itba.paw.models.types.Sport;
import java.time.Instant;

public class MatchUtils {

    public static Match createMatchWithId(
            Long matchId, Long hostId, Sport sport, Instant startsAt, Integer maxPlayers) {
        return new Match(
                matchId,
                sport,
                UserUtils.getUser(hostId),
                "Address",
                null,
                null,
                "Title",
                "Desc",
                startsAt,
                startsAt.plusSeconds(3600),
                maxPlayers,
                null,
                EventVisibility.PUBLIC,
                EventJoinPolicy.DIRECT,
                EventStatus.OPEN,
                0,
                null,
                null,
                null,
                false,
                null,
                null,
                null);
    }

    public static MatchSeries getMatchSeries(Long seriesId, User host) {
        return new MatchSeries(seriesId, host, null, null, null, null, null, null, null, null);
    }
}
