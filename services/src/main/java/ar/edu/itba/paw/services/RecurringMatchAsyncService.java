package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.ImageMetadata;
import ar.edu.itba.paw.models.MatchSeries;
import ar.edu.itba.paw.models.types.EventJoinPolicy;
import ar.edu.itba.paw.models.types.EventVisibility;
import ar.edu.itba.paw.persistence.MatchDao;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecurringMatchAsyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecurringMatchAsyncService.class);

    private final MatchDao matchDao;

    @Autowired
    public RecurringMatchAsyncService(final MatchDao matchDao) {
        this.matchDao = matchDao;
    }

    @Async("matchRecurrenceTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createSeriesOccurrencesAsync(
            final CreateMatchRequest request,
            final MatchSeries series,
            final ImageMetadata bannerImageMetadata,
            final List<OccurrenceWindowData> occurrences,
            final int startIndex) {
        try {
            for (int i = 0; i < occurrences.size(); i++) {
                final OccurrenceWindowData occurrence = occurrences.get(i);
                matchDao.createMatch(
                        request.getHost(),
                        request.getAddress(),
                        request.getTitle(),
                        request.getDescription(),
                        occurrence.startsAt(),
                        occurrence.endsAt(),
                        request.getMaxPlayers(),
                        request.getPricePerPlayer(),
                        request.getSport(),
                        request.getVisibility(),
                        resolveJoinPolicy(request.getVisibility(), request.getJoinPolicy()),
                        request.getStatus(),
                        bannerImageMetadata,
                        request.getLatitude(),
                        request.getLongitude(),
                        series,
                        startIndex + i);
            }
        } catch (final RuntimeException exception) {
            LOGGER.error(
                    "Recurring occurrence creation failed seriesId={} startIndex={} occurrences={}",
                    series != null ? series.getId() : null,
                    startIndex,
                    occurrences != null ? occurrences.size() : 0,
                    exception);
            throw exception;
        }
    }

    private static EventJoinPolicy resolveJoinPolicy(
            final EventVisibility visibility, final EventJoinPolicy joinPolicy) {
        return EventVisibility.PRIVATE == visibility ? EventJoinPolicy.INVITE_ONLY : joinPolicy;
    }

    public record OccurrenceWindowData(Instant startsAt, Instant endsAt) {}
}
