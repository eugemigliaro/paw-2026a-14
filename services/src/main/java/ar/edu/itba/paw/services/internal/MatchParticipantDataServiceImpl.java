package ar.edu.itba.paw.services.internal;

import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.MatchParticipantDao;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MatchParticipantDataServiceImpl implements MatchParticipantDataService {

    private final MatchParticipantDao matchParticipantDao;

    @Autowired
    public MatchParticipantDataServiceImpl(final MatchParticipantDao matchParticipantDao) {
        this.matchParticipantDao = Objects.requireNonNull(matchParticipantDao);
    }

    @Override
    public List<User> findConfirmedParticipants(final Long matchId) {
        return matchParticipantDao.findConfirmedParticipants(matchId);
    }
}
