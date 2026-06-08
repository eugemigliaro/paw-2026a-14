package ar.edu.itba.paw.webapp.utils;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.MatchParticipationService;
import ar.edu.itba.paw.services.MatchReservationService;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class EventCardAttributeUtils {

    private EventCardAttributeUtils() {}

    public static Map<Long, String> matchSpotsBadgeCodes(final List<Match> matches) {
        final Map<Long, String> codes = new LinkedHashMap<>();
        for (final Match match : matches) {
            codes.put(match.getId(), "event.spotsLeft");
        }
        return codes;
    }

    public static Map<Long, String> matchStatusBadgeCodes(final List<Match> matches) {
        final Map<Long, String> codes = new LinkedHashMap<>();
        for (final Match match : matches) {
            codes.put(match.getId(), "match.status." + match.getStatus().getValue());
        }
        return codes;
    }

    public static Map<Long, String> tournamentBadgeCodes(final List<Tournament> tournaments) {
        final Map<Long, String> codes = new LinkedHashMap<>();
        for (final Tournament tournament : tournaments) {
            codes.put(tournament.getId(), "tournament.card.badge");
        }
        return codes;
    }

    public static Map<Long, String> matchDistanceLabels(
            final List<Match> matches,
            final Double latitude,
            final Double longitude,
            final Locale locale) {
        final Map<Long, String> labels = new LinkedHashMap<>();
        for (final Match match : matches) {
            final String label = distanceLabel(match.getDistanceKmFromViewer(), locale);
            if (label != null) {
                labels.put(match.getId(), label);
            }
        }
        return labels;
    }

    public static Map<Long, String> tournamentDistanceLabels(
            final List<Tournament> tournaments,
            final Double latitude,
            final Double longitude,
            final Locale locale) {
        final Map<Long, String> labels = new LinkedHashMap<>();
        for (final Tournament tournament : tournaments) {
            final String label = distanceLabel(tournament.getDistanceKmFromViewer(), locale);
            if (label != null) {
                labels.put(tournament.getId(), label);
            }
        }
        return labels;
    }

    public static Map<Long, List<String>> matchRelationshipBadgeCodes(
            final List<Match> matches,
            final User currentUser,
            final MatchParticipationService matchParticipationService,
            final MatchReservationService matchReservationService) {
        final Map<Long, List<String>> codes = new LinkedHashMap<>();
        for (final Match match : matches) {
            final List<String> matchCodes =
                    matchRelationshipBadgeCodes(
                            match, currentUser, matchParticipationService, matchReservationService);
            if (!matchCodes.isEmpty()) {
                codes.put(match.getId(), matchCodes);
            }
        }
        return codes;
    }

    public static Map<Long, List<String>> tournamentRelationshipBadgeCodes(
            final List<Tournament> tournaments, final User currentUser) {
        final Map<Long, List<String>> codes = new LinkedHashMap<>();
        for (final Tournament tournament : tournaments) {
            final List<String> tournamentCodes =
                    tournamentRelationshipBadgeCodes(tournament, currentUser);
            if (!tournamentCodes.isEmpty()) {
                codes.put(tournament.getId(), tournamentCodes);
            }
        }
        return codes;
    }

    private static String distanceLabel(final Double distanceKm, final Locale locale) {
        if (distanceKm == null) {
            return null;
        }
        final Locale resolvedLocale = locale == null ? Locale.ENGLISH : locale;
        final NumberFormat formatter = NumberFormat.getNumberInstance(resolvedLocale);
        if (distanceKm < 1) {
            formatter.setMaximumFractionDigits(0);
            return formatter.format(Math.max(1, Math.round(distanceKm * 1000))) + " m";
        }
        formatter.setMinimumFractionDigits(distanceKm < 10 ? 1 : 0);
        formatter.setMaximumFractionDigits(distanceKm < 10 ? 1 : 0);
        return formatter.format(distanceKm) + " km";
    }

    private static List<String> matchRelationshipBadgeCodes(
            final Match match,
            final User currentUser,
            final MatchParticipationService matchParticipationService,
            final MatchReservationService matchReservationService) {
        if (currentUser == null) {
            return List.of();
        }
        final List<String> codes = new ArrayList<>();
        if (currentUser.getId().equals(match.getHost().getId())) {
            codes.add("my_event");
        }
        if (matchParticipationService.hasPendingRequest(match.getId(), currentUser)) {
            codes.add("pending");
        } else if (matchParticipationService.hasInvitation(match.getId(), currentUser)) {
            codes.add("invited");
        } else if (matchReservationService.hasActiveReservation(match.getId(), currentUser)) {
            codes.add("going");
        }
        return List.copyOf(codes);
    }

    private static List<String> tournamentRelationshipBadgeCodes(
            final Tournament tournament, final User currentUser) {
        if (currentUser == null
                || tournament == null
                || tournament.getHost() == null
                || tournament.getHost().getId() == null) {
            return List.of();
        }
        return currentUser.getId().equals(tournament.getHost().getId())
                ? List.of("my_event")
                : List.of();
    }
}
