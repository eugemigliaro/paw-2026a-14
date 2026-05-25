package ar.edu.itba.paw.webapp.controller;

import static ar.edu.itba.paw.webapp.utils.ImageUrlHelper.profileUrlFor;
import static ar.edu.itba.paw.webapp.utils.ViewFormatUtils.dateFormatter;
import static ar.edu.itba.paw.webapp.utils.ViewFormatUtils.formatInstant;
import static ar.edu.itba.paw.webapp.utils.ViewFormatUtils.priceLabel;
import static ar.edu.itba.paw.webapp.utils.ViewFormatUtils.sportLabel;
import static ar.edu.itba.paw.webapp.utils.ViewFormatUtils.timeFormatter;

import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.TournamentSoloEntry;
import ar.edu.itba.paw.models.TournamentTeam;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.types.TournamentSoloEntryStatus;
import ar.edu.itba.paw.models.types.TournamentStatus;
import ar.edu.itba.paw.models.types.UserRole;
import ar.edu.itba.paw.services.TournamentJoinFailureReason;
import ar.edu.itba.paw.services.TournamentRegistrationService;
import ar.edu.itba.paw.services.TournamentService;
import ar.edu.itba.paw.services.exceptions.TournamentRegistrationException;
import ar.edu.itba.paw.webapp.security.CurrentAuthenticatedUser;
import ar.edu.itba.paw.webapp.utils.SecurityControllerUtils;
import ar.edu.itba.paw.webapp.viewmodel.ShellViewModelFactory;
import ar.edu.itba.paw.webapp.viewmodel.TournamentDetailViewModel;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
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
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class TournamentController {

    private final TournamentService tournamentService;
    private final TournamentRegistrationService tournamentRegistrationService;
    private final MessageSource messageSource;

    public TournamentController(
            final TournamentService tournamentService,
            final TournamentRegistrationService tournamentRegistrationService,
            final MessageSource messageSource) {
        this.tournamentService = tournamentService;
        this.tournamentRegistrationService = tournamentRegistrationService;
        this.messageSource = messageSource;
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
            redirectAttributes.addFlashAttribute(
                    "tournamentNoticeCode", "tournament.registration.joined");
        } catch (final TournamentRegistrationException exception) {
            handleRegistrationException(exception, redirectAttributes);
        }
        return new ModelAndView("redirect:/tournaments/" + tournamentId);
    }

    @PostMapping("/tournaments/{tournamentId:\\d+}/solo-entry/leave")
    @PreAuthorize("isAuthenticated()")
    public ModelAndView leaveSolo(
            @PathVariable("tournamentId") final Long tournamentId,
            final RedirectAttributes redirectAttributes) {
        final User currentUser = SecurityControllerUtils.requireAuthenticatedUser();
        try {
            tournamentRegistrationService.leaveSolo(tournamentId, currentUser);
            redirectAttributes.addFlashAttribute(
                    "tournamentNoticeCode", "tournament.registration.left");
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
        final boolean registrationOpen = TournamentStatus.REGISTRATION == tournament.getStatus();
        final TournamentSoloEntryStatus soloStatus =
                soloEntry.map(TournamentSoloEntry::getStatus).orElse(null);
        final boolean canJoinSolo =
                currentUser != null
                        && registrationOpen
                        && tournament.isAllowSoloSignup()
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
                registrationOpen && (isHost(tournament, currentUser) || isAdminMod());

        return new TournamentDetailViewModel(
                tournament.getId(),
                tournament.getTitle(),
                tournament.getDescription(),
                sportLabel(tournament.getSport(), locale, messageSource),
                statusLabel(tournament, locale),
                tournament.getStatus().getDbValue().replace('_', '-'),
                tournament.getAddress(),
                scheduleLabel(tournament, locale),
                registrationWindowLabel(tournament, locale),
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
                registrationOpen,
                tournament.isAllowSoloSignup(),
                canJoinSolo,
                canLeaveSolo,
                requiresLoginToJoin,
                canCloseRegistration);
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

    private String registrationWindowLabel(final Tournament tournament, final Locale locale) {
        final Instant opensAt = tournament.getRegistrationOpensAt();
        final Instant closesAt = tournament.getRegistrationClosesAt();
        if (opensAt == null || closesAt == null) {
            return messageSource.getMessage(
                    "tournament.detail.registrationWindow.tbd", null, locale);
        }
        return messageSource.getMessage(
                "tournament.detail.registrationWindow.value",
                new Object[] {formatInstant(opensAt, locale), formatInstant(closesAt, locale)},
                locale);
    }

    private String joinModeLabel(final Tournament tournament, final Locale locale) {
        if (tournament.isAllowSoloSignup()) {
            return messageSource.getMessage("tournament.detail.joinMode.solo", null, locale);
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
                    new Object[] {userTeam.get().getName()},
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
                            new Object[] {assignedTeam.getName()},
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
            case FORBIDDEN:
                return "tournament.registration.error.forbidden";
            case TOURNAMENT_NOT_FOUND:
            default:
                return "tournament.registration.error.notFound";
        }
    }

    private static Optional<String> flashString(final Model model, final String name) {
        final Object value = model.asMap().get(name);
        return value instanceof String ? Optional.of((String) value) : Optional.empty();
    }
}
