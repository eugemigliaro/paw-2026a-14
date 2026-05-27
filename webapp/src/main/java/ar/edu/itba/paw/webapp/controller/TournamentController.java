package ar.edu.itba.paw.webapp.controller;

import static ar.edu.itba.paw.webapp.utils.ImageUrlHelper.profileUrlFor;
import static ar.edu.itba.paw.webapp.utils.ViewFormatUtils.dateFormatter;
import static ar.edu.itba.paw.webapp.utils.ViewFormatUtils.formatInstant;
import static ar.edu.itba.paw.webapp.utils.ViewFormatUtils.priceLabel;
import static ar.edu.itba.paw.webapp.utils.ViewFormatUtils.sportLabel;
import static ar.edu.itba.paw.webapp.utils.ViewFormatUtils.timeFormatter;

import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.TournamentMatch;
import ar.edu.itba.paw.models.TournamentSoloEntry;
import ar.edu.itba.paw.models.TournamentTeam;
import ar.edu.itba.paw.models.TournamentTeamMember;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.types.TournamentSoloEntryStatus;
import ar.edu.itba.paw.models.types.TournamentStatus;
import ar.edu.itba.paw.models.types.TournamentTeamOrigin;
import ar.edu.itba.paw.models.types.UserRole;
import ar.edu.itba.paw.services.TournamentBracketFailureReason;
import ar.edu.itba.paw.services.TournamentBracketService;
import ar.edu.itba.paw.services.TournamentBracketView;
import ar.edu.itba.paw.services.TournamentJoinFailureReason;
import ar.edu.itba.paw.services.TournamentRegistrationReadiness;
import ar.edu.itba.paw.services.TournamentRegistrationService;
import ar.edu.itba.paw.services.TournamentService;
import ar.edu.itba.paw.services.TournamentWinnerDeclarationRequest;
import ar.edu.itba.paw.services.exceptions.TournamentBracketException;
import ar.edu.itba.paw.services.exceptions.TournamentRegistrationException;
import ar.edu.itba.paw.webapp.security.CurrentAuthenticatedUser;
import ar.edu.itba.paw.webapp.utils.SecurityControllerUtils;
import ar.edu.itba.paw.webapp.viewmodel.ShellViewModelFactory;
import ar.edu.itba.paw.webapp.viewmodel.TournamentBracketViewModel;
import ar.edu.itba.paw.webapp.viewmodel.TournamentDetailViewModel;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final Clock clock;

    public TournamentController(
            final TournamentService tournamentService,
            final TournamentRegistrationService tournamentRegistrationService,
            final TournamentBracketService tournamentBracketService,
            final MessageSource messageSource,
            final Clock clock) {
        this.tournamentService = tournamentService;
        this.tournamentRegistrationService = tournamentRegistrationService;
        this.tournamentBracketService = tournamentBracketService;
        this.messageSource = messageSource;
        this.clock = clock;
    }

    @GetMapping("/tournaments/{tournamentId:\\d+}")
    public ModelAndView showTournament(
            @PathVariable("tournamentId") final Long tournamentId,
            final Model model,
            final Locale locale) {
        final Tournament tournament =
                tournamentService
                        .findPublicTournament(tournamentId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        final User currentUser = SecurityControllerUtils.currentUserOrNull();

        final Optional<TournamentSoloEntry> soloEntry =
                tournamentRegistrationService.findSoloEntry(tournamentId, currentUser);
        final Optional<TournamentTeam> userTeam =
                tournamentRegistrationService.findUserTeam(tournamentId, currentUser);

        final ModelAndView mav = new ModelAndView("tournaments/detail");
        mav.addObject(
                "pageTitle",
                messageSource.getMessage(
                        "page.title.tournamentDetail",
                        new Object[] {tournament.getTitle()},
                        locale));
        mav.addObject("shell", ShellViewModelFactory.playerShell(messageSource, locale));
        mav.addObject(
                "tournamentPage",
                buildTournamentPage(tournament, currentUser, soloEntry, userTeam, locale));
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
    @PreAuthorize("isAuthenticated()")
    public ModelAndView joinSolo(
            @PathVariable("tournamentId") final Long tournamentId,
            final RedirectAttributes redirectAttributes) {
        final User currentUser = SecurityControllerUtils.requireAuthenticatedUser();
        try {
            tournamentRegistrationService.joinSolo(tournamentId, currentUser);
        } catch (final TournamentRegistrationException exception) {
            handleRegistrationException(exception, redirectAttributes);
        }
        return new ModelAndView("redirect:/tournaments/" + tournamentId);
    }

    @GetMapping("/tournaments/{tournamentId:\\d+}/bracket")
    public ModelAndView showBracket(
            @PathVariable("tournamentId") final Long tournamentId,
            final Model model,
            final Locale locale) {
        final User currentUser = SecurityControllerUtils.currentUserOrNull();
        final TournamentBracketView bracketView;
        try {
            bracketView = tournamentBracketService.getBracket(tournamentId, currentUser);
        } catch (final TournamentBracketException exception) {
            if (TournamentBracketFailureReason.TOURNAMENT_NOT_FOUND == exception.getReason()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
            if (TournamentBracketFailureReason.FORBIDDEN == exception.getReason()
                    || TournamentBracketFailureReason.BRACKET_NOT_GENERATED
                            == exception.getReason()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
            throw exception;
        }

        final ModelAndView mav = new ModelAndView("tournaments/bracket");
        mav.addObject(
                "pageTitle",
                messageSource.getMessage(
                        "page.title.tournamentBracket",
                        new Object[] {bracketView.getTournament().getTitle()},
                        locale));
        mav.addObject("shell", ShellViewModelFactory.playerShell(messageSource, locale));
        mav.addObject("bracketPage", buildBracketPage(bracketView, locale));
        mav.addObject("tournamentDetailPath", "/tournaments/" + tournamentId);
        mav.addObject(
                "matchDatesSetupPath",
                canDefineMatchDates(bracketView.getTournament(), currentUser)
                        ? "/host/tournaments/" + tournamentId + "/bracket/setup"
                        : null);
        mav.addObject(
                "tournamentNoticeCode", flashString(model, "tournamentNoticeCode").orElse(null));
        mav.addObject(
                "tournamentErrorCode", flashString(model, "tournamentErrorCode").orElse(null));
        return mav;
    }

    @PostMapping("/host/tournaments/{tournamentId:\\d+}/matches/{matchId:\\d+}/winner")
    @PreAuthorize("isAuthenticated()")
    public ModelAndView declareWinner(
            @PathVariable("tournamentId") final Long tournamentId,
            @PathVariable("matchId") final Long matchId,
            @RequestParam("winnerTeamId") final Long winnerTeamId,
            final RedirectAttributes redirectAttributes) {
        final User actingUser = SecurityControllerUtils.requireAuthenticatedUser();
        try {
            tournamentBracketService.declareWinner(
                    tournamentId,
                    matchId,
                    new TournamentWinnerDeclarationRequest(winnerTeamId),
                    actingUser);
            redirectAttributes.addFlashAttribute(
                    "tournamentNoticeCode", "tournament.bracket.result.saved");
        } catch (final TournamentBracketException exception) {
            handleBracketMutationException(exception, redirectAttributes);
        }
        return new ModelAndView("redirect:/tournaments/" + tournamentId + "/bracket");
    }

    @PostMapping("/tournaments/{tournamentId:\\d+}/solo-entry/leave")
    @PreAuthorize("isAuthenticated()")
    public ModelAndView leaveSolo(
            @PathVariable("tournamentId") final Long tournamentId,
            final RedirectAttributes redirectAttributes) {
        final User currentUser = SecurityControllerUtils.requireAuthenticatedUser();
        try {
            tournamentRegistrationService.leaveSolo(tournamentId, currentUser);
        } catch (final TournamentRegistrationException exception) {
            handleRegistrationException(exception, redirectAttributes);
        }
        return new ModelAndView("redirect:/tournaments/" + tournamentId);
    }

    private TournamentDetailViewModel buildTournamentPage(
            final Tournament tournament,
            final User currentUser,
            final Optional<TournamentSoloEntry> soloEntry,
            final Optional<TournamentTeam> userTeam,
            final Locale locale) {
        final Instant now = Instant.now(clock);
        final boolean registrationOpen = isRegistrationOpenNow(tournament, now);
        final boolean registrationNotStarted = isRegistrationNotStarted(tournament, now);
        final TournamentSoloEntryStatus soloStatus =
                soloEntry.map(TournamentSoloEntry::getStatus).orElse(null);
        final boolean canJoinSolo =
                currentUser != null
                        && registrationOpen
                        && tournament.isAllowSoloSignup()
                        && !tournamentRegistrationService.isSoloPoolFull(tournament.getId())
                        && userTeam.isEmpty()
                        && soloStatus != TournamentSoloEntryStatus.IN_POOL
                        && soloStatus != TournamentSoloEntryStatus.ASSIGNED;
        final boolean canLeaveSolo =
                currentUser != null
                        && registrationOpen
                        && soloStatus == TournamentSoloEntryStatus.IN_POOL;
        final boolean requiresLoginToJoin =
                currentUser == null && registrationOpen && tournament.isAllowSoloSignup();
        final boolean canCloseRegistration =
                TournamentStatus.REGISTRATION == tournament.getStatus()
                        && (isHost(tournament, currentUser) || isAdminMod());
        final boolean canEditTournament =
                TournamentStatus.REGISTRATION == tournament.getStatus()
                        && (isHost(tournament, currentUser) || isAdminMod());
        final boolean canCancelTournament =
                TournamentStatus.COMPLETED != tournament.getStatus()
                        && TournamentStatus.CANCELLED != tournament.getStatus()
                        && (isHost(tournament, currentUser) || isAdminMod());
        final boolean canManageBracket =
                TournamentStatus.BRACKET_SETUP == tournament.getStatus()
                        && (isHost(tournament, currentUser) || isAdminMod());
        final boolean canViewBracket =
                TournamentStatus.IN_PROGRESS == tournament.getStatus()
                        || TournamentStatus.COMPLETED == tournament.getStatus()
                        || TournamentStatus.CANCELLED == tournament.getStatus();
        final TournamentRegistrationReadiness readiness =
                canCloseRegistration
                        ? tournamentRegistrationService.getRegistrationReadiness(
                                tournament.getId(), currentUser)
                        : null;

        return new TournamentDetailViewModel(
                tournament.getId(),
                tournament.getTitle(),
                tournament.getDescription(),
                sportLabel(tournament.getSport(), locale, messageSource),
                statusLabel(tournament, locale),
                tournament.getStatus().getDbValue().replace('_', '-'),
                tournament.getAddress(),
                scheduleLabel(tournament, locale),
                registrationWindowStartLabel(tournament, locale),
                registrationWindowEndLabel(tournament, locale),
                messageSource.getMessage(
                        "tournament.detail.bracketSize.value",
                        new Object[] {tournament.getBracketSize()},
                        locale),
                messageSource.getMessage(
                        "tournament.detail.teamSize.value",
                        new Object[] {tournament.getTeamSize()},
                        locale),
                messageSource.getMessage(
                        "tournament.format." + tournament.getFormat().getDbValue(), null, locale),
                joinModeLabel(tournament, locale),
                priceLabel(tournament.getPricePerPlayer(), locale, messageSource),
                hostLabel(tournament.getHost(), locale),
                hostProfileHref(tournament.getHost()),
                profileUrlFor(tournament.getHost()),
                bannerUrlFor(tournament),
                participationLabel(soloEntry, userTeam, locale),
                nextStepLabel(tournament, locale),
                aboutParagraphs(tournament, locale),
                participantRows(tournament, locale),
                closeRegistrationConfirmMessage(readiness, locale),
                registrationOpen,
                tournament.isAllowSoloSignup(),
                canJoinSolo,
                canLeaveSolo,
                requiresLoginToJoin,
                registrationNotStarted,
                canCloseRegistration,
                canEditTournament,
                canCancelTournament,
                canManageBracket,
                canViewBracket);
    }

    private String closeRegistrationConfirmMessage(
            final TournamentRegistrationReadiness readiness, final Locale locale) {
        if (readiness == null || !readiness.isCancellationRisk()) {
            return null;
        }
        return messageSource.getMessage(
                "tournament.host.closeRegistration.cancelConfirm",
                new Object[] {readiness.getFinalTeamCount()},
                locale);
    }

    private List<TournamentDetailViewModel.ParticipantViewModel> participantRows(
            final Tournament tournament, final Locale locale) {
        if (TournamentStatus.REGISTRATION != tournament.getStatus()) {
            return List.of();
        }
        final List<TournamentDetailViewModel.ParticipantViewModel> rows =
                new java.util.ArrayList<>();
        for (final TournamentTeamMember member :
                tournamentRegistrationService.listTeamMembers(tournament.getId())) {
            rows.add(
                    new TournamentDetailViewModel.ParticipantViewModel(
                            hostLabel(member.getUser(), locale),
                            teamName(member.getTeam(), locale)));
        }
        for (final TournamentSoloEntry entry :
                tournamentRegistrationService.listActiveSoloEntries(tournament.getId())) {
            rows.add(
                    new TournamentDetailViewModel.ParticipantViewModel(
                            hostLabel(entry.getUser(), locale),
                            messageSource.getMessage(
                                    "tournament.participants.soloPool", null, locale)));
        }
        return rows;
    }

    private void handleRegistrationException(
            final TournamentRegistrationException exception,
            final RedirectAttributes redirectAttributes) {
        if (TournamentJoinFailureReason.TOURNAMENT_NOT_FOUND == exception.getReason()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (TournamentJoinFailureReason.FORBIDDEN == exception.getReason()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        redirectAttributes.addFlashAttribute(
                "tournamentErrorCode", registrationErrorCode(exception.getReason()));
    }

    private String statusLabel(final Tournament tournament, final Locale locale) {
        return messageSource.getMessage(
                "tournament.status." + tournament.getStatus().getDbValue(), null, locale);
    }

    private String scheduleLabel(final Tournament tournament, final Locale locale) {
        if (tournament.getStartsAt() == null) {
            return messageSource.getMessage("tournament.detail.schedule.tbd", null, locale);
        }
        if (tournament.getEndsAt() == null) {
            return formatInstant(tournament.getStartsAt(), locale);
        }

        final ZoneId zoneId = ZoneId.systemDefault();
        final LocalDateTime startsAt = LocalDateTime.ofInstant(tournament.getStartsAt(), zoneId);
        final LocalDateTime endsAt = LocalDateTime.ofInstant(tournament.getEndsAt(), zoneId);
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
                    formatInstant(tournament.getStartsAt(), locale),
                    formatInstant(tournament.getEndsAt(), locale)
                },
                locale);
    }

    private String registrationWindowStartLabel(final Tournament tournament, final Locale locale) {
        return formatInstant(tournament.getRegistrationOpensAt(), locale);
    }

    private String registrationWindowEndLabel(final Tournament tournament, final Locale locale) {
        return formatInstant(tournament.getRegistrationClosesAt(), locale);
    }

    private boolean isRegistrationOpenNow(final Tournament tournament, final Instant now) {
        final Instant opensAt = tournament.getRegistrationOpensAt();
        final Instant closesAt = tournament.getRegistrationClosesAt();
        return TournamentStatus.REGISTRATION == tournament.getStatus()
                && opensAt != null
                && closesAt != null
                && !now.isBefore(opensAt)
                && now.isBefore(closesAt);
    }

    private boolean isRegistrationNotStarted(final Tournament tournament, final Instant now) {
        final Instant opensAt = tournament.getRegistrationOpensAt();
        return TournamentStatus.REGISTRATION == tournament.getStatus()
                && opensAt != null
                && now.isBefore(opensAt);
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
            final Locale locale) {
        if (userTeam.isPresent()) {
            return messageSource.getMessage(
                    "tournament.participation.team",
                    new Object[] {teamName(userTeam.get(), locale)},
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
                            new Object[] {teamName(assignedTeam, locale)},
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

    private static String bannerUrlFor(final Tournament tournament) {
        return tournament.hasBannerImage()
                ? "/images/" + tournament.getBannerImageMetadata().getId()
                : null;
    }

    private static boolean isHost(final Tournament tournament, final User currentUser) {
        return tournament.getHost() != null
                && currentUser != null
                && tournament.getHost().getId().equals(currentUser.getId());
    }

    private static boolean canDefineMatchDates(
            final Tournament tournament, final User currentUser) {
        return TournamentStatus.BRACKET_SETUP == tournament.getStatus()
                && (isHost(tournament, currentUser) || isAdminMod());
    }

    private static boolean isAdminMod() {
        return CurrentAuthenticatedUser.get()
                .map(principal -> UserRole.ADMIN_MOD == principal.getRole())
                .orElse(false);
    }

    private static String registrationErrorCode(final TournamentJoinFailureReason reason) {
        switch (reason) {
            case SOLO_SIGNUP_DISABLED:
                return "tournament.registration.error.soloDisabled";
            case REGISTRATION_NOT_OPEN:
                return "tournament.registration.error.notOpen";
            case ALREADY_IN_SOLO_POOL:
                return "tournament.registration.error.alreadyInSoloPool";
            case ALREADY_ON_TEAM:
                return "tournament.registration.error.alreadyOnTeam";
            case ALREADY_ASSIGNED:
                return "tournament.registration.error.alreadyAssigned";
            case NOT_IN_SOLO_POOL:
                return "tournament.registration.error.notInSoloPool";
            case SOLO_POOL_FULL:
                return "tournament.registration.error.soloPoolFull";
            case FORBIDDEN:
                return "tournament.registration.error.forbidden";
            case TOURNAMENT_NOT_FOUND:
            default:
                return "tournament.registration.error.notFound";
        }
    }

    private void handleBracketMutationException(
            final TournamentBracketException exception,
            final RedirectAttributes redirectAttributes) {
        if (TournamentBracketFailureReason.TOURNAMENT_NOT_FOUND == exception.getReason()
                || TournamentBracketFailureReason.MATCH_NOT_FOUND == exception.getReason()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (TournamentBracketFailureReason.FORBIDDEN == exception.getReason()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        redirectAttributes.addFlashAttribute(
                "tournamentErrorCode", bracketErrorCode(exception.getReason()));
    }

    private static String bracketErrorCode(final TournamentBracketFailureReason reason) {
        switch (reason) {
            case MATCH_NOT_FOUND:
                return "tournament.bracket.error.matchNotFound";
            case NOT_READY_FOR_BRACKET:
                return "tournament.bracket.error.notReady";
            case BRACKET_ALREADY_GENERATED:
                return "tournament.bracket.error.alreadyGenerated";
            case BRACKET_NOT_GENERATED:
                return "tournament.bracket.error.notGenerated";
            case UNDER_CAPACITY:
                return "tournament.bracket.error.underCapacity";
            case MISSING_MATCH_SCHEDULE:
                return "tournament.bracket.error.missingMatchSchedule";
            case INVALID_SCHEDULE:
                return "tournament.bracket.error.invalidSchedule";
            case SCHEDULE_BEFORE_NOW:
                return "tournament.bracket.error.beforeNow";
            case INVALID_ROUND_ORDER:
                return "tournament.bracket.error.invalidRoundOrder";
            case NOT_IN_PROGRESS:
                return "tournament.bracket.error.notInProgress";
            case MATCH_NOT_READY:
                return "tournament.bracket.error.matchNotReady";
            case MATCH_ALREADY_DECIDED:
                return "tournament.bracket.error.matchAlreadyDecided";
            case WINNER_NOT_IN_MATCH:
                return "tournament.bracket.error.winnerNotInMatch";
            case FORBIDDEN:
                return "tournament.bracket.error.forbidden";
            case TOURNAMENT_NOT_FOUND:
            case TEAM_NOT_FOUND:
            default:
                return "tournament.bracket.error.notFound";
        }
    }

    private TournamentBracketViewModel buildBracketPage(
            final TournamentBracketView bracketView, final Locale locale) {
        final Tournament tournament = bracketView.getTournament();
        final Long focusedMatchId =
                bracketView.getFocusedMatch() == null
                        ? null
                        : bracketView.getFocusedMatch().getId();
        final Long viewerTeamId =
                bracketView.getViewerTeam() == null ? null : bracketView.getViewerTeam().getId();
        final boolean canManageResults =
                TournamentStatus.IN_PROGRESS == tournament.getStatus()
                        && (isHost(tournament, SecurityControllerUtils.currentUserOrNull())
                                || isAdminMod());
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
        final List<TournamentBracketViewModel.RoundViewModel> rounds =
                matchesByRound.entrySet().stream()
                        .map(
                                entry ->
                                        new TournamentBracketViewModel.RoundViewModel(
                                                entry.getKey(),
                                                roundLabel(
                                                        entry.getKey(),
                                                        matchesByRound.size(),
                                                        locale),
                                                entry.getValue().stream()
                                                        .map(
                                                                match ->
                                                                        new TournamentBracketViewModel
                                                                                .MatchViewModel(
                                                                                match.getId(),
                                                                                teamId(
                                                                                        match
                                                                                                .getTeamA()),
                                                                                teamId(
                                                                                        match
                                                                                                .getTeamB()),
                                                                                matchLabel(
                                                                                        match,
                                                                                        locale),
                                                                                teamName(
                                                                                        match
                                                                                                .getTeamA(),
                                                                                        locale),
                                                                                teamName(
                                                                                        match
                                                                                                .getTeamB(),
                                                                                        locale),
                                                                                matchStatusLabel(
                                                                                        match,
                                                                                        locale),
                                                                                Objects.equals(
                                                                                        focusedMatchId,
                                                                                        match
                                                                                                .getId()),
                                                                                sameTeam(
                                                                                        match
                                                                                                .getTeamA(),
                                                                                        viewerTeamId),
                                                                                sameTeam(
                                                                                        match
                                                                                                .getTeamB(),
                                                                                        viewerTeamId),
                                                                                canManageResults
                                                                                        && canRecordResult(
                                                                                                match),
                                                                                matchScheduleLabel(
                                                                                        match,
                                                                                        locale),
                                                                                "",
                                                                                "",
                                                                                "",
                                                                                "",
                                                                                "",
                                                                                "",
                                                                                ""))
                                                        .toList()))
                        .toList();

        return new TournamentBracketViewModel(
                tournament.getId(),
                tournament.getTitle(),
                statusLabel(tournament, locale),
                tournament.getStatus().getDbValue().replace('_', '-'),
                focusedMatchLabel(bracketView.getFocusedMatch(), locale),
                focusedMatchTeamsLabel(bracketView.getFocusedMatch(), locale),
                matchScheduleLabel(bracketView.getFocusedMatch(), locale),
                bracketView.getFocusedMatch() == null
                        ? null
                        : bracketView.getFocusedMatch().getAddress(),
                true,
                false,
                canManageResults,
                rounds);
    }

    private String roundLabel(final int roundNumber, final int roundCount, final Locale locale) {
        if (roundNumber == roundCount) {
            return messageSource.getMessage("tournament.bracket.round.final", null, locale);
        }
        return messageSource.getMessage(
                "tournament.bracket.round.number", new Object[] {roundNumber}, locale);
    }

    private String matchLabel(final TournamentMatch match, final Locale locale) {
        return messageSource.getMessage(
                "tournament.bracket.match.label", new Object[] {match.getMatchIndex() + 1}, locale);
    }

    private String teamName(final TournamentTeam team, final Locale locale) {
        if (team == null) {
            return messageSource.getMessage("tournament.bracket.team.tbd", null, locale);
        }
        if (team.getName() != null
                && !team.getName().isBlank()
                && !isLegacyGeneratedSoloTeamName(team)) {
            return team.getName();
        }
        if (team.getId() == null) {
            return messageSource.getMessage("tournament.bracket.team.tbd", null, locale);
        }
        return messageSource.getMessage(
                "tournament.team.solo.name", new Object[] {team.getId()}, locale);
    }

    private static boolean isLegacyGeneratedSoloTeamName(final TournamentTeam team) {
        if (team.getOrigin() != TournamentTeamOrigin.SOLO_POOL || team.getName() == null) {
            return false;
        }
        final String normalized = team.getName().trim();
        return normalized.matches("(?i)Solo squad #\\d+")
                || normalized.matches("Equipo individual #\\d+");
    }

    private static Long teamId(final TournamentTeam team) {
        return team == null ? null : team.getId();
    }

    private static boolean canRecordResult(final TournamentMatch match) {
        return match.getTeamA() != null
                && match.getTeamB() != null
                && match.getWinnerTeam() == null;
    }

    private String matchStatusLabel(final TournamentMatch match, final Locale locale) {
        return messageSource.getMessage(
                "tournament.match.status." + match.getStatus().getDbValue(), null, locale);
    }

    private String matchScheduleLabel(final TournamentMatch match, final Locale locale) {
        if (match == null || match.getScheduledStartsAt() == null) {
            return messageSource.getMessage("tournament.bracket.schedule.tbd", null, locale);
        }
        if (match.getScheduledEndsAt() == null) {
            return formatInstant(match.getScheduledStartsAt(), locale);
        }
        return messageSource.getMessage(
                "tournament.bracket.schedule.range",
                new Object[] {
                    formatInstant(match.getScheduledStartsAt(), locale),
                    formatInstant(match.getScheduledEndsAt(), locale)
                },
                locale);
    }

    private String focusedMatchLabel(final TournamentMatch match, final Locale locale) {
        return match == null ? null : matchLabel(match, locale);
    }

    private String focusedMatchTeamsLabel(final TournamentMatch match, final Locale locale) {
        if (match == null) {
            return null;
        }
        return messageSource.getMessage(
                "tournament.bracket.match.teams",
                new Object[] {
                    teamName(match.getTeamA(), locale), teamName(match.getTeamB(), locale)
                },
                locale);
    }

    private static boolean sameTeam(final TournamentTeam team, final Long teamId) {
        return team != null && team.getId() != null && Objects.equals(team.getId(), teamId);
    }

    private static Optional<String> flashString(final Model model, final String name) {
        final Object value = model.asMap().get(name);
        return value instanceof String ? Optional.of((String) value) : Optional.empty();
    }
}
