package ar.edu.itba.paw.webapp.controller;

import static ar.edu.itba.paw.webapp.utils.ImageUrlHelper.bannerUrlFor;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.Sport;
import ar.edu.itba.paw.services.MatchParticipationService;
import ar.edu.itba.paw.services.exceptions.MatchParticipationException;
import ar.edu.itba.paw.webapp.security.AuthenticatedUserPrincipal;
import ar.edu.itba.paw.webapp.security.CurrentAuthenticatedUser;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.EventCardViewModel;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.InvitedMatchViewModel;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.PendingJoinMatchViewModel;
import ar.edu.itba.paw.webapp.viewmodel.ShellViewModelFactory;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class PlayerParticipationController {

    private final MatchParticipationService matchParticipationService;
    private final MessageSource messageSource;

    @Autowired
    public PlayerParticipationController(
            final MatchParticipationService matchParticipationService,
            final MessageSource messageSource) {
        this.matchParticipationService = Objects.requireNonNull(matchParticipationService);
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @GetMapping("/player/matches/requests")
    public ModelAndView showPendingRequests(final Locale locale) {
        final long userId = requireAuthenticatedUserId();
        final List<Match> pendingMatches =
                matchParticipationService.findPendingRequestMatches(userId);

        final ModelAndView mav = new ModelAndView("player/participation/requests");
        final Locale resolvedLocale = locale == null ? Locale.ENGLISH : locale;
        mav.addObject(
                "shell",
                ShellViewModelFactory.playerShell(
                        messageSource, resolvedLocale, "/player/matches/requests"));
        mav.addObject("pendingMatches", toPendingJoinViewModels(pendingMatches, resolvedLocale));
        mav.addObject(
                "emptyMessage",
                messageSource.getMessage("player.requests.empty", null, resolvedLocale));
        return mav;
    }

    @PostMapping("/matches/{matchId}/join-requests")
    public ModelAndView requestToJoin(
            @PathVariable("matchId") final String matchId, final Locale locale) {
        final long userId = requireAuthenticatedUserId();
        final long resolvedMatchId = parseMatchIdOrThrow(matchId);

        try {
            matchParticipationService.requestToJoin(resolvedMatchId, userId);
            return new ModelAndView("redirect:/matches/" + resolvedMatchId + "?join=requested");
        } catch (final MatchParticipationException e) {
            return new ModelAndView(
                    "redirect:/matches/" + resolvedMatchId + "?joinError=" + e.getCode());
        }
    }

    @PostMapping("/matches/{matchId}/join-requests/cancel")
    public ModelAndView cancelJoinRequest(
            @PathVariable("matchId") final String matchId, final Locale locale) {
        final long userId = requireAuthenticatedUserId();
        final long resolvedMatchId = parseMatchIdOrThrow(matchId);

        try {
            matchParticipationService.cancelJoinRequest(resolvedMatchId, userId);
            return new ModelAndView("redirect:/matches/" + resolvedMatchId + "?join=cancelled");
        } catch (final MatchParticipationException e) {
            return new ModelAndView(
                    "redirect:/matches/" + resolvedMatchId + "?joinError=" + e.getCode());
        }
    }

    @GetMapping("/player/matches/invites")
    public ModelAndView showInvites(final Locale locale) {
        final long userId = requireAuthenticatedUserId();
        final java.util.List<Match> invitedMatches =
                matchParticipationService.findInvitedMatches(userId);

        final ModelAndView mav = new ModelAndView("player/participation/invites");
        final Locale resolvedLocale = locale == null ? Locale.ENGLISH : locale;
        mav.addObject(
                "shell",
                ShellViewModelFactory.playerShell(
                        messageSource, resolvedLocale, "/player/matches/invites"));
        mav.addObject("invitedMatches", toInvitedMatchViewModels(invitedMatches, resolvedLocale));
        mav.addObject(
                "emptyMessage",
                messageSource.getMessage("player.invites.empty", null, resolvedLocale));
        return mav;
    }

    @PostMapping("/matches/{matchId}/invites/accept")
    public ModelAndView acceptInvite(
            @PathVariable("matchId") final String matchId, final Locale locale) {
        final long userId = requireAuthenticatedUserId();
        final long resolvedMatchId = parseMatchIdOrThrow(matchId);

        try {
            matchParticipationService.acceptInvite(resolvedMatchId, userId);
            return new ModelAndView("redirect:/matches/" + resolvedMatchId + "?invite=accepted");
        } catch (final MatchParticipationException e) {
            return new ModelAndView(
                    "redirect:/matches/" + resolvedMatchId + "?inviteError=" + e.getCode());
        }
    }

    @PostMapping("/matches/{matchId}/invites/decline")
    public ModelAndView declineInvite(
            @PathVariable("matchId") final String matchId, final Locale locale) {
        final long userId = requireAuthenticatedUserId();
        final long resolvedMatchId = parseMatchIdOrThrow(matchId);

        try {
            matchParticipationService.declineInvite(resolvedMatchId, userId);
            return new ModelAndView("redirect:/player/matches/invites?invite=declined");
        } catch (final MatchParticipationException e) {
            return new ModelAndView(
                    "redirect:/matches/" + resolvedMatchId + "?inviteError=" + e.getCode());
        }
    }

    private java.util.List<InvitedMatchViewModel> toInvitedMatchViewModels(
            final java.util.List<Match> matches, final Locale locale) {
        return matches.stream()
                .map(
                        m ->
                                new InvitedMatchViewModel(
                                        toCard(m, locale),
                                        "/matches/" + m.getId() + "/invites/accept",
                                        "/matches/" + m.getId() + "/invites/decline"))
                .toList();
    }

    private List<PendingJoinMatchViewModel> toPendingJoinViewModels(
            final List<Match> matches, final Locale locale) {
        return matches.stream()
                .map(
                        m ->
                                new PendingJoinMatchViewModel(
                                        toCard(m, locale),
                                        "/matches/" + m.getId() + "/join-requests/cancel"))
                .toList();
    }

    private EventCardViewModel toCard(final Match match, final Locale locale) {
        final Locale resolved = locale == null ? Locale.ENGLISH : locale;
        String cardHref = "/matches/" + match.getId();
        return new EventCardViewModel(
                String.valueOf(match.getId()),
                cardHref,
                messageSource.getMessage(
                        "sport." + match.getSport().getDbValue(),
                        null,
                        match.getSport().getDisplayName(),
                        resolved),
                match.getTitle(),
                match.getAddress(),
                DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                        .withLocale(resolved)
                        .format(match.getStartsAt().atZone(ZoneId.systemDefault())),
                toPriceLabel(match.getPricePerPlayer(), locale),
                messageSource.getMessage(
                        "match.status." + match.getStatus(), null, match.getStatus(), resolved),
                null,
                mediaClassFor(match.getSport()),
                bannerUrlFor(match));
    }

    private String toPriceLabel(final BigDecimal price, final Locale locale) {
        if (price == null) {
            return messageSource.getMessage("price.tbd", null, locale);
        }
        return price.compareTo(BigDecimal.ZERO) == 0
                ? messageSource.getMessage("price.free", null, locale)
                : messageSource.getMessage("price.amount", new Object[] {price}, locale);
    }

    private static String mediaClassFor(final Sport sport) {
        switch (sport) {
            case FOOTBALL:
                return "media-tile--football";
            case TENNIS:
                return "media-tile--tennis";
            case BASKETBALL:
                return "media-tile--basketball";
            case PADEL:
            default:
                return "media-tile--padel";
        }
    }

    private static long requireAuthenticatedUserId() {
        return CurrentAuthenticatedUser.get()
                .map(AuthenticatedUserPrincipal::getUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private static long parseMatchIdOrThrow(final String raw) {
        try {
            return Long.parseLong(raw);
        } catch (final NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}
