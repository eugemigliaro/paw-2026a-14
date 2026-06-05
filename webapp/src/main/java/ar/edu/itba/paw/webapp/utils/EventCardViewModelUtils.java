package ar.edu.itba.paw.webapp.utils;

import static ar.edu.itba.paw.webapp.utils.ImageUrlHelper.bannerUrlFor;
import static ar.edu.itba.paw.webapp.utils.ViewFormatUtils.mediaClassFor;
import static ar.edu.itba.paw.webapp.utils.ViewFormatUtils.priceLabel;
import static ar.edu.itba.paw.webapp.utils.ViewFormatUtils.recurringLabel;
import static ar.edu.itba.paw.webapp.utils.ViewFormatUtils.resolvedLocale;
import static ar.edu.itba.paw.webapp.utils.ViewFormatUtils.scheduleFormatter;
import static ar.edu.itba.paw.webapp.utils.ViewFormatUtils.sportLabel;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.PlatformTime;
import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.MatchParticipationService;
import ar.edu.itba.paw.services.MatchReservationService;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.EventCardViewModel;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.EventRelationshipBadgeViewModel;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.context.MessageSource;

public final class EventCardViewModelUtils {

    private EventCardViewModelUtils() {}

    public static EventCardViewModel toCard(
            final Match match,
            final Locale locale,
            final User currentUser,
            final String badge,
            final MessageSource messageSource,
            final MatchParticipationService matchParticipationService,
            final MatchReservationService matchReservationService) {
        return toCard(
                match,
                locale,
                currentUser,
                badge,
                null,
                messageSource,
                matchParticipationService,
                matchReservationService);
    }

    public static EventCardViewModel toCard(
            final Match match,
            final Locale locale,
            final User currentUser,
            final String badge,
            final String distanceLabel,
            final MessageSource messageSource,
            final MatchParticipationService matchParticipationService,
            final MatchReservationService matchReservationService) {
        final Locale resolvedLocale = resolvedLocale(locale);
        final ZonedDateTime startsAt = match.getStartsAt().atZone(PlatformTime.ZONE);
        final List<EventRelationshipBadgeViewModel> relationshipBadges =
                relationshipBadgesFor(
                        match,
                        currentUser,
                        locale,
                        messageSource,
                        matchParticipationService,
                        matchReservationService);

        return new EventCardViewModel(
                String.valueOf(match.getId()),
                "/matches/" + match.getId(),
                sportLabel(match.getSport(), resolvedLocale, messageSource),
                match.getTitle(),
                match.getAddress(),
                hostLabelFor(match),
                scheduleFormatter(resolvedLocale).format(startsAt),
                DateTimeFormatter.ofPattern("EEE, MMM d", resolvedLocale).format(startsAt),
                DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                        .withLocale(resolvedLocale)
                        .format(startsAt),
                priceLabel(match.getPricePerPlayer(), locale, messageSource),
                badge,
                relationshipBadges,
                recurringLabel(match, locale, messageSource),
                null,
                distanceLabel,
                mediaClassFor(match.getSport()),
                bannerUrlFor(match));
    }

    public static EventCardViewModel toCard(
            final Tournament tournament,
            final Locale locale,
            final User currentUser,
            final String badge,
            final String statusLabel,
            final String distanceLabel,
            final MessageSource messageSource) {
        final Locale resolvedLocale = resolvedLocale(locale);
        if (tournament.getStartsAt() == null) {
            return new EventCardViewModel(
                    String.valueOf(tournament.getId()),
                    "/tournaments/" + tournament.getId(),
                    sportLabel(tournament.getSport(), resolvedLocale, messageSource),
                    tournament.getTitle(),
                    tournament.getAddress(),
                    hostLabelFor(tournament),
                    messageSource.getMessage("tournament.detail.schedule.tbd", null, locale),
                    "",
                    "",
                    priceLabel(tournament.getPricePerPlayer(), locale, messageSource),
                    badge,
                    tournamentRelationshipBadges(tournament, currentUser, locale, messageSource),
                    null,
                    statusLabel,
                    distanceLabel,
                    mediaClassFor(tournament.getSport()),
                    bannerUrlFor(tournament));
        }

        final Instant scheduleInstant = tournament.getStartsAt();
        final ZonedDateTime startsAt = scheduleInstant.atZone(PlatformTime.ZONE);

        return new EventCardViewModel(
                String.valueOf(tournament.getId()),
                "/tournaments/" + tournament.getId(),
                sportLabel(tournament.getSport(), resolvedLocale, messageSource),
                tournament.getTitle(),
                tournament.getAddress(),
                hostLabelFor(tournament),
                scheduleFormatter(resolvedLocale).format(startsAt),
                DateTimeFormatter.ofPattern("EEE, MMM d", resolvedLocale).format(startsAt),
                DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                        .withLocale(resolvedLocale)
                        .format(startsAt),
                priceLabel(tournament.getPricePerPlayer(), locale, messageSource),
                badge,
                tournamentRelationshipBadges(tournament, currentUser, locale, messageSource),
                null,
                statusLabel,
                distanceLabel,
                mediaClassFor(tournament.getSport()),
                bannerUrlFor(tournament));
    }

    public static String hostLabelFor(final Match match) {
        if (match == null || match.getHost().getId() == null) {
            return null;
        }
        return match.getHost().getUsername();
    }

    public static String hostLabelFor(final Tournament tournament) {
        if (tournament == null || tournament.getHost().getId() == null) {
            return null;
        }
        return tournament.getHost().getUsername();
    }

    private static List<EventRelationshipBadgeViewModel> tournamentRelationshipBadges(
            final Tournament tournament,
            final User currentUser,
            final Locale locale,
            final MessageSource messageSource) {
        if (currentUser == null
                || tournament == null
                || tournament.getHost() == null
                || tournament.getHost().getId() == null) {
            return List.of();
        }
        return currentUser.getId().equals(tournament.getHost().getId())
                ? List.of(relationshipBadge("my_event", locale, messageSource))
                : List.of();
    }

    public static List<EventRelationshipBadgeViewModel> relationshipBadgesFor(
            final Match match,
            final User currentUser,
            final Locale locale,
            final MessageSource messageSource,
            final MatchParticipationService matchParticipationService,
            final MatchReservationService matchReservationService) {
        if (currentUser == null) {
            return List.of();
        }
        final List<EventRelationshipBadgeViewModel> badges = new ArrayList<>();
        if (currentUser.getId().equals(match.getHost().getId())) {
            badges.add(relationshipBadge("my_event", locale, messageSource));
        }
        if (matchParticipationService.hasPendingRequest(match.getId(), currentUser)) {
            badges.add(relationshipBadge("pending", locale, messageSource));
        } else if (matchParticipationService.hasInvitation(match.getId(), currentUser)) {
            badges.add(relationshipBadge("invited", locale, messageSource));
        } else if (matchReservationService.hasActiveReservation(match.getId(), currentUser)) {
            badges.add(relationshipBadge("going", locale, messageSource));
        }
        return List.copyOf(badges);
    }

    public static EventRelationshipBadgeViewModel relationshipBadge(
            final String type, final Locale locale, final MessageSource messageSource) {
        return new EventRelationshipBadgeViewModel(
                type, messageSource.getMessage("event.relationship." + type, null, locale));
    }
}
