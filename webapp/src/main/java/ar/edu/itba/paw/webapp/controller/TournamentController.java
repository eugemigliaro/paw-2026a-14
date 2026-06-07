package ar.edu.itba.paw.webapp.controller;

import static ar.edu.itba.paw.webapp.utils.ImageUrlHelper.bannerUrlFor;
import static ar.edu.itba.paw.webapp.utils.ImageUrlHelper.profileUrlFor;
import static ar.edu.itba.paw.webapp.utils.ViewFormatUtils.dateFormatter;
import static ar.edu.itba.paw.webapp.utils.ViewFormatUtils.formatInstant;
import static ar.edu.itba.paw.webapp.utils.ViewFormatUtils.priceLabel;
import static ar.edu.itba.paw.webapp.utils.ViewFormatUtils.timeFormatter;

import ar.edu.itba.paw.models.PlatformTime;
import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.TournamentMatch;
import ar.edu.itba.paw.models.TournamentSoloEntry;
import ar.edu.itba.paw.models.TournamentTeam;
import ar.edu.itba.paw.models.TournamentTeamMember;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.exceptions.tournamentBracket.TournamentBracketException;
import ar.edu.itba.paw.models.exceptions.tournamentBracket.TournamentBracketNotGeneratedException;
import ar.edu.itba.paw.models.exceptions.tournamentRegistration.TournamentRegistrationException;
import ar.edu.itba.paw.models.types.TournamentSoloEntryStatus;
import ar.edu.itba.paw.models.types.TournamentStatus;
import ar.edu.itba.paw.services.TournamentBracketService;
import ar.edu.itba.paw.services.TournamentBracketView;
import ar.edu.itba.paw.services.TournamentRegistrationService;
import ar.edu.itba.paw.services.TournamentService;
import ar.edu.itba.paw.services.TournamentViewerCapabilities;
import ar.edu.itba.paw.services.TournamentWinnerDeclarationRequest;
import ar.edu.itba.paw.webapp.security.annotation.AuthenticatedUser;
import ar.edu.itba.paw.webapp.security.annotation.CurrentUser;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class TournamentController {

    private final TournamentService tournamentService;
    private final TournamentRegistrationService tournamentRegistrationService;
    private final TournamentBracketService tournamentBracketService;
    private final MessageSource messageSource;

    @Autowired
    public TournamentController(
            final TournamentService tournamentService,
            final TournamentRegistrationService tournamentRegistrationService,
            final TournamentBracketService tournamentBracketService,
            final MessageSource messageSource) {
        this.tournamentService = tournamentService;
        this.tournamentRegistrationService = tournamentRegistrationService;
        this.tournamentBracketService = tournamentBracketService;
        this.messageSource = messageSource;
    }

    @GetMapping("/tournaments/{tournamentId:\\d+}")
    public ModelAndView showTournament(
            @CurrentUser final User user,
            @PathVariable("tournamentId") final Long tournamentId,
            final Model model,
            final Locale locale) {
        final Tournament tournament =
                tournamentService
                        .findPublicTournament(tournamentId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        final Optional<TournamentSoloEntry> soloEntry =
                tournamentRegistrationService.findSoloEntry(tournamentId, user);
        final Optional<TournamentTeam> userTeam =
                tournamentRegistrationService.findUserTeam(tournamentId, user);

        final ModelAndView mav = new ModelAndView("tournaments/detail");
        mav.addObject(
                "pageTitle",
                messageSource.getMessage(
                        "page.title.tournamentDetail",
                        new Object[] {tournament.getTitle()},
                        locale));
        addTournamentDetailModel(mav, tournament, user, soloEntry, userTeam, locale);
        mav.addObject("soloJoinPath", "/tournaments/" + tournamentId + "/solo-entry");
        mav.addObject("soloLeavePath", "/tournaments/" + tournamentId + "/solo-entry/leave");
        mav.addObject(
                "closeRegistrationPath",
                "/host/tournaments/" + tournamentId + "/close-registration");
        mav.addObject("editTournamentPath", "/host/tournaments/" + tournamentId + "/edit");
        mav.addObject("cancelTournamentPath", "/host/tournaments/" + tournamentId + "/cancel");
        mav.addObject(
                "tournamentNoticeCode", flashString(model, "tournamentNoticeCode").orElse(null));
        mav.addObject(
                "tournamentErrorCode", flashString(model, "tournamentErrorCode").orElse(null));
        return mav;
    }

    @PostMapping("/tournaments/{tournamentId:\\d+}/solo-entry")
    public ModelAndView joinSolo(
            @AuthenticatedUser final User user,
            @PathVariable("tournamentId") final Long tournamentId,
            final RedirectAttributes redirectAttributes) {
        try {
            tournamentRegistrationService.joinSolo(tournamentId, user);
        } catch (TournamentRegistrationException e) {
            final String tournamentErrorCode = "tournament.registration.error." + e.getMessage();
            redirectAttributes.addFlashAttribute("tournamentErrorCode", tournamentErrorCode);
        }
        return new ModelAndView("redirect:/tournaments/" + tournamentId);
    }

    @GetMapping("/tournaments/{tournamentId:\\d+}/bracket")
    public ModelAndView showBracket(
            @CurrentUser final User user,
            @PathVariable("tournamentId") final Long tournamentId,
            final Model model,
            final Locale locale) {
        TournamentBracketView bracketView;
        try {
            bracketView = tournamentBracketService.getBracket(tournamentId, user);
        } catch (final TournamentBracketNotGeneratedException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }

        final ModelAndView mav = new ModelAndView("tournaments/bracket");
        mav.addObject(
                "pageTitle",
                messageSource.getMessage(
                        "page.title.tournamentBracket",
                        new Object[] {bracketView.getTournament().getTitle()},
                        locale));
        addBracketModel(mav, user, bracketView, locale);
        mav.addObject("tournamentDetailPath", "/tournaments/" + tournamentId);
        mav.addObject(
                "matchDatesSetupPath",
                tournamentService
                                .viewerCapabilities(bracketView.getTournament(), user)
                                .isCanManageBracket()
                        ? "/host/tournaments/" + tournamentId + "/bracket/setup"
                        : null);
        mav.addObject(
                "tournamentNoticeCode", flashString(model, "tournamentNoticeCode").orElse(null));
        mav.addObject(
                "tournamentErrorCode", flashString(model, "tournamentErrorCode").orElse(null));
        return mav;
    }

    @PostMapping("/host/tournaments/{tournamentId:\\d+}/matches/{matchId:\\d+}/winner")
    public ModelAndView declareWinner(
            @AuthenticatedUser final User user,
            @PathVariable("tournamentId") final Long tournamentId,
            @PathVariable("matchId") final Long matchId,
            @RequestParam("winnerTeamId") final Long winnerTeamId,
            final RedirectAttributes redirectAttributes) {
        try {
            tournamentBracketService.declareWinner(
                    tournamentId,
                    matchId,
                    new TournamentWinnerDeclarationRequest(winnerTeamId),
                    user);
            redirectAttributes.addFlashAttribute(
                    "tournamentNoticeCode", "tournament.bracket.result.saved");
        } catch (final TournamentBracketException e) {
            final String errorCode = "tournament.bracket.error." + e.getMessage();
            redirectAttributes.addFlashAttribute("tournamentErrorCode", errorCode);
        }

        return new ModelAndView("redirect:/tournaments/" + tournamentId + "/bracket");
    }

    @PostMapping("/tournaments/{tournamentId:\\d+}/solo-entry/leave")
    public ModelAndView leaveSolo(
            @AuthenticatedUser final User user,
            @PathVariable("tournamentId") final Long tournamentId,
            final RedirectAttributes redirectAttributes) {
        try {
            tournamentRegistrationService.leaveSolo(tournamentId, user);
        } catch (final TournamentRegistrationException e) {
            final String errorCode = "tournament.registration.error." + e.getMessage();
            redirectAttributes.addFlashAttribute("tournamentErrorCode", errorCode);
        }

        return new ModelAndView("redirect:/tournaments/" + tournamentId);
    }

    private void addTournamentDetailModel(
            final ModelAndView mav,
            final Tournament tournament,
            final User currentUser,
            final Optional<TournamentSoloEntry> soloEntry,
            final Optional<TournamentTeam> userTeam,
            final Locale locale) {
        final TournamentViewerCapabilities capabilities =
                tournamentService.viewerCapabilities(tournament, currentUser);
        final List<TournamentTeamMember> teamMembers =
                tournamentRegistrationService.listTeamMembers(tournament.getId());
        final Map<Long, Integer> teamDisplayNumbers = teamDisplayNumbersFromMembers(teamMembers);

        mav.addObject("tournament", tournament);
        mav.addObject("tournamentCapabilities", capabilities);
        mav.addObject("tournamentScheduleLabel", scheduleLabel(tournament, locale));
        mav.addObject(
                "tournamentRegistrationWindowStartLabel",
                registrationWindowStartLabel(tournament, locale));
        mav.addObject(
                "tournamentRegistrationWindowEndLabel",
                registrationWindowEndLabel(tournament, locale));
        mav.addObject("tournamentJoinModeLabel", joinModeLabel(tournament, locale));
        mav.addObject(
                "tournamentPriceLabel",
                priceLabel(tournament.getPricePerPlayer(), locale, messageSource));
        mav.addObject("tournamentHostLabel", hostLabel(tournament.getHost(), locale));
        mav.addObject("tournamentHostProfileHref", hostProfileHref(tournament.getHost()));
        mav.addObject("tournamentHostProfileImageUrl", profileUrlFor(tournament.getHost()));
        mav.addObject("tournamentBannerImageUrl", bannerUrlFor(tournament));
        mav.addObject(
                "tournamentParticipationLabel",
                participationLabel(soloEntry, userTeam, locale, teamDisplayNumbers));
        mav.addObject("tournamentNextStepLabel", nextStepLabel(tournament, locale));
        mav.addObject("tournamentAboutParagraphs", aboutParagraphs(tournament, locale));
        mav.addObject("tournamentTeamMembers", teamMembers);
        mav.addObject(
                "tournamentActiveSoloEntries",
                tournamentRegistrationService.listActiveSoloEntries(tournament.getId()));
        mav.addObject("tournamentTeamDisplayNumbers", teamDisplayNumbers);
        mav.addObject(
                "tournamentCloseRegistrationDisabledMessage",
                closeRegistrationDisabledMessage(capabilities, locale));
    }

    private String closeRegistrationDisabledMessage(
            final TournamentViewerCapabilities capabilities, final Locale locale) {
        if (!capabilities.isCloseRegistrationBlockedByCapacity()) {
            return null;
        }
        return messageSource.getMessage(
                "tournament.host.closeRegistration.unavailable", null, locale);
    }

    private String scheduleLabel(final Tournament tournament, final Locale locale) {
        if (tournament.getStartsAt() == null) {
            return messageSource.getMessage("tournament.detail.schedule.tbd", null, locale);
        }
        if (tournament.getEndsAt() == null) {
            return formatInstant(tournament.getStartsAt(), locale, PlatformTime.ZONE);
        }

        final OffsetDateTime startsAt = tournament.getStartsAtDateTime();
        final OffsetDateTime endsAt = tournament.getEndsAtDateTime();
        if (startsAt.toLocalDate().equals(endsAt.toLocalDate())) {
            return messageSource.getMessage(
                    "tournament.detail.schedule.sameDay",
                    new Object[] {
                        dateFormatter(locale).format(startsAt.toLocalDate()),
                        timeFormatter(locale).format(startsAt.toLocalTime()),
                        timeFormatter(locale).format(endsAt.toLocalTime())
                    },
                    locale);
        }
        return messageSource.getMessage(
                "tournament.detail.schedule.range",
                new Object[] {
                    formatInstant(tournament.getStartsAt(), locale, PlatformTime.ZONE),
                    formatInstant(tournament.getEndsAt(), locale, PlatformTime.ZONE)
                },
                locale);
    }

    private String registrationWindowStartLabel(final Tournament tournament, final Locale locale) {
        return formatInstant(tournament.getRegistrationOpensAt(), locale, PlatformTime.ZONE);
    }

    private String registrationWindowEndLabel(final Tournament tournament, final Locale locale) {
        return formatInstant(tournament.getRegistrationClosesAt(), locale, PlatformTime.ZONE);
    }

    private String joinModeLabel(final Tournament tournament, final Locale locale) {
        if (tournament.isAllowSoloSignup() && tournament.isAllowTeamDraft()) {
            return messageSource.getMessage("tournament.detail.joinMode.both", null, locale);
        }
        if (tournament.isAllowSoloSignup()) {
            return messageSource.getMessage("tournament.detail.joinMode.solo", null, locale);
        }
        if (tournament.isAllowTeamDraft()) {
            return messageSource.getMessage("tournament.detail.joinMode.teamDraft", null, locale);
        }
        return messageSource.getMessage("tournament.detail.joinMode.closed", null, locale);
    }

    private String participationLabel(
            final Optional<TournamentSoloEntry> soloEntry,
            final Optional<TournamentTeam> userTeam,
            final Locale locale,
            final Map<Long, Integer> teamDisplayNumbers) {
        if (userTeam.isPresent()) {
            return messageSource.getMessage(
                    "tournament.participation.team",
                    new Object[] {teamName(userTeam.get(), locale, teamDisplayNumbers)},
                    locale);
        }
        if (soloEntry.isEmpty()) {
            return null;
        }
        final TournamentSoloEntry entry = soloEntry.get();
        if (TournamentSoloEntryStatus.IN_POOL == entry.getStatus()) {
            return messageSource.getMessage("tournament.participation.soloPool", null, locale);
        }
        if (TournamentSoloEntryStatus.ASSIGNED == entry.getStatus()) {
            final TournamentTeam assignedTeam = entry.getAssignedTeam();
            return assignedTeam == null
                    ? messageSource.getMessage("tournament.participation.assigned", null, locale)
                    : messageSource.getMessage(
                            "tournament.participation.assignedTeam",
                            new Object[] {teamName(assignedTeam, locale, teamDisplayNumbers)},
                            locale);
        }
        if (TournamentSoloEntryStatus.UNASSIGNED == entry.getStatus()) {
            return messageSource.getMessage("tournament.participation.unassigned", null, locale);
        }
        if (TournamentSoloEntryStatus.LEFT == entry.getStatus()) {
            return messageSource.getMessage("tournament.participation.left", null, locale);
        }
        return null;
    }

    private String nextStepLabel(final Tournament tournament, final Locale locale) {
        if (TournamentStatus.BRACKET_SETUP == tournament.getStatus()) {
            return messageSource.getMessage("tournament.nextStep.bracketSetup", null, locale);
        }
        if (TournamentStatus.CANCELLED == tournament.getStatus()) {
            return messageSource.getMessage("tournament.nextStep.cancelled", null, locale);
        }
        if (TournamentStatus.IN_PROGRESS == tournament.getStatus()) {
            return messageSource.getMessage("tournament.nextStep.inProgress", null, locale);
        }
        if (TournamentStatus.COMPLETED == tournament.getStatus()) {
            return messageSource.getMessage("tournament.nextStep.completed", null, locale);
        }
        return null;
    }

    private List<String> aboutParagraphs(final Tournament tournament, final Locale locale) {
        final String description = tournament.getDescription();
        if (description == null || description.isBlank()) {
            return List.of(
                    messageSource.getMessage("tournament.detail.defaultDescription", null, locale));
        }
        return Arrays.stream(description.split("\\R+"))
                .map(String::trim)
                .filter(paragraph -> !paragraph.isEmpty())
                .collect(Collectors.toList());
    }

    private String hostLabel(final User host, final Locale locale) {
        if (host == null || host.getUsername() == null || host.getUsername().isBlank()) {
            return messageSource.getMessage(
                    "event.detail.unknownHost",
                    new Object[] {host == null ? "?" : host.getId()},
                    locale);
        }
        return host.getUsername();
    }

    private String hostProfileHref(final User host) {
        if (host == null || host.getUsername() == null || host.getUsername().isBlank()) {
            return null;
        }
        return "/users/" + host.getUsername();
    }

    private void addBracketModel(
            final ModelAndView mav,
            final User currentUser,
            final TournamentBracketView bracketView,
            final Locale locale) {
        final Tournament tournament = bracketView.getTournament();
        final Long viewerTeamId =
                bracketView.getViewerTeam() == null ? null : bracketView.getViewerTeam().getId();
        final boolean canManageResults =
                tournamentService.viewerCapabilities(tournament, currentUser).isCanManageResults();
        final Map<Integer, List<TournamentMatch>> matchesByRound =
                bracketView.getMatches().stream()
                        .sorted(
                                Comparator.comparingInt(TournamentMatch::getRoundNumber)
                                        .thenComparingInt(TournamentMatch::getMatchIndex))
                        .collect(
                                Collectors.groupingBy(
                                        TournamentMatch::getRoundNumber,
                                        LinkedHashMap::new,
                                        Collectors.toList()));
        final Map<Long, Integer> teamDisplayNumbers = teamDisplayNumbers(bracketView.getTeams());
        final int totalRounds = matchesByRound.size();
        final Map<Long, List<String>> usernamesByTeamId = new LinkedHashMap<>();
        for (final TournamentTeamMember member : bracketView.getTeamMembers()) {
            if (member.getTeam() == null || member.getUser() == null) {
                continue;
            }
            usernamesByTeamId
                    .computeIfAbsent(member.getTeam().getId(), ignored -> new ArrayList<>())
                    .add(member.getUser().getUsername());
        }
        mav.addObject("bracketView", bracketView);
        mav.addObject("bracketTournament", tournament);
        mav.addObject("bracketMatchesByRound", matchesByRound);
        mav.addObject("bracketRoundCount", totalRounds);
        mav.addObject("bracketTeamDisplayNumbers", teamDisplayNumbers);
        mav.addObject("bracketMembersByTeamId", usernamesByTeamId);
        mav.addObject("bracketViewerTeamId", viewerTeamId);
        mav.addObject("bracketCanManageResults", canManageResults);
    }

    private String teamName(
            final TournamentTeam team,
            final Locale locale,
            final Map<Long, Integer> teamDisplayNumbers) {
        if (team == null) {
            return messageSource.getMessage("tournament.bracket.team.tbd", null, locale);
        }
        if (team.getName() != null && !team.getName().isBlank()) {
            return team.getName();
        }
        if (team.getId() == null) {
            return messageSource.getMessage("tournament.bracket.team.tbd", null, locale);
        }
        final Integer displayNumber = teamDisplayNumbers.get(team.getId());
        return messageSource.getMessage(
                "tournament.team.solo.name",
                new Object[] {displayNumber == null ? team.getId() : displayNumber},
                locale);
    }

    private static Map<Long, Integer> teamDisplayNumbers(final List<TournamentTeam> teams) {
        if (teams == null || teams.isEmpty()) {
            return Map.of();
        }
        final Map<Long, Integer> displayNumbers = new LinkedHashMap<>();
        for (int index = 0; index < teams.size(); index++) {
            final TournamentTeam team = teams.get(index);
            if (team != null && team.getId() != null) {
                displayNumbers.put(team.getId(), index + 1);
            }
        }
        return displayNumbers;
    }

    private static Map<Long, Integer> teamDisplayNumbersFromMembers(
            final List<TournamentTeamMember> teamMembers) {
        if (teamMembers == null || teamMembers.isEmpty()) {
            return Map.of();
        }
        final Map<Long, Integer> displayNumbers = new LinkedHashMap<>();
        for (final TournamentTeamMember member : teamMembers) {
            if (member == null || member.getTeam() == null || member.getTeam().getId() == null) {
                continue;
            }
            displayNumbers.computeIfAbsent(
                    member.getTeam().getId(), ignored -> displayNumbers.size() + 1);
        }
        return displayNumbers;
    }

    private static Optional<String> flashString(final Model model, final String name) {
        final Object value = model.asMap().get(name);
        return value instanceof String ? Optional.of((String) value) : Optional.empty();
    }
}
