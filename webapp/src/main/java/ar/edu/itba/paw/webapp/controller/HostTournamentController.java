package ar.edu.itba.paw.webapp.controller;

import static ar.edu.itba.paw.webapp.utils.ViewFormatUtils.formatInstant;

import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.TournamentMatch;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.types.PersistableEnum;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.models.types.TournamentFormat;
import ar.edu.itba.paw.models.types.TournamentStatus;
import ar.edu.itba.paw.services.CreateTournamentRequest;
import ar.edu.itba.paw.services.TournamentBracketFailureReason;
import ar.edu.itba.paw.services.TournamentBracketService;
import ar.edu.itba.paw.services.TournamentBracketView;
import ar.edu.itba.paw.services.TournamentJoinFailureReason;
import ar.edu.itba.paw.services.TournamentLifecycleFailureReason;
import ar.edu.itba.paw.services.TournamentMatchScheduleRequest;
import ar.edu.itba.paw.services.TournamentRegistrationService;
import ar.edu.itba.paw.services.TournamentService;
import ar.edu.itba.paw.services.exceptions.TournamentBracketException;
import ar.edu.itba.paw.services.exceptions.TournamentLifecycleException;
import ar.edu.itba.paw.services.exceptions.TournamentRegistrationException;
import ar.edu.itba.paw.webapp.form.CreateTournamentForm;
import ar.edu.itba.paw.webapp.utils.SecurityControllerUtils;
import ar.edu.itba.paw.webapp.viewmodel.ShellViewModelFactory;
import ar.edu.itba.paw.webapp.viewmodel.TournamentBracketViewModel;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("isAuthenticated()")
public class HostTournamentController {

    private static final List<Integer> SUPPORTED_BRACKET_SIZES = List.of(4, 8, 16);
    private static final Map<Sport, List<Integer>> SUPPORTED_TEAM_SIZES_BY_SPORT =
            Map.of(
                    Sport.PADEL,
                    List.of(1, 2),
                    Sport.TENNIS,
                    List.of(1, 2),
                    Sport.FOOTBALL,
                    List.of(5, 7, 8, 11),
                    Sport.BASKETBALL,
                    List.of(3, 5),
                    Sport.OTHER,
                    List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11));
    private static final DateTimeFormatter TIME_INPUT_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm");

    private final TournamentService tournamentService;
    private final TournamentRegistrationService tournamentRegistrationService;
    private final TournamentBracketService tournamentBracketService;
    private final MessageSource messageSource;
    private final Clock clock;

    public HostTournamentController(
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

    @ModelAttribute("createTournamentForm")
    public CreateTournamentForm createTournamentForm() {
        return new CreateTournamentForm();
    }

    @GetMapping("/host/tournaments/new")
    public ModelAndView showCreateTournament(final Locale locale) {
        SecurityControllerUtils.requireAuthenticatedUser();
        return createFormView(createTournamentForm(), null, locale);
    }

    @PostMapping("/host/tournaments")
    public ModelAndView createTournament(
            @Valid @ModelAttribute("createTournamentForm")
                    final CreateTournamentForm createTournamentForm,
            final BindingResult bindingResult,
            final Locale locale) {
        final User actingUser = SecurityControllerUtils.requireAuthenticatedUser();

        applyFormValidation(createTournamentForm, bindingResult, locale);

        if (bindingResult.hasErrors()) {
            return createFormView(createTournamentForm, null, locale);
        }

        final Sport sport =
                PersistableEnum.fromDbValue(Sport.class, createTournamentForm.getSport())
                        .orElse(Sport.PADEL);
        final CreateTournamentRequest request =
                new CreateTournamentRequest(
                        sport,
                        createTournamentForm.getTitle(),
                        createTournamentForm.getDescription(),
                        createTournamentForm.getAddress(),
                        parseCoordinate(createTournamentForm.getLatitude()),
                        parseCoordinate(createTournamentForm.getLongitude()),
                        null,
                        null,
                        createTournamentForm.getPricePerPlayer(),
                        null,
                        TournamentFormat.SINGLE_ELIMINATION,
                        createTournamentForm.getBracketSize(),
                        createTournamentForm.getTeamSize(),
                        createTournamentForm.isAllowSoloSignup(),
                        createTournamentForm.isAllowTeamDraft(),
                        toInstant(
                                createTournamentForm.getRegistrationOpensDate(),
                                createTournamentForm.getRegistrationOpensTime(),
                                createTournamentForm.getTz()),
                        toInstant(
                                createTournamentForm.getRegistrationClosesDate(),
                                createTournamentForm.getRegistrationClosesTime(),
                                createTournamentForm.getTz()));

        try {
            final Tournament createdTournament =
                    tournamentService.createTournament(actingUser, request);
            return new ModelAndView("redirect:/tournaments/" + createdTournament.getId());
        } catch (final TournamentLifecycleException exception) {
            applyServiceError(exception, bindingResult, locale);
            return createFormView(createTournamentForm, null, locale);
        } catch (final IllegalArgumentException exception) {
            return createFormView(createTournamentForm, exception.getMessage(), locale);
        }
    }

    @PostMapping("/host/tournaments/{tournamentId:\\d+}/close-registration")
    public ModelAndView closeRegistration(
            @PathVariable("tournamentId") final Long tournamentId,
            final RedirectAttributes redirectAttributes) {
        final User actingUser = SecurityControllerUtils.requireAuthenticatedUser();

        try {
            final Tournament tournament =
                    tournamentRegistrationService.closeRegistration(tournamentId, actingUser);
            if (TournamentStatus.BRACKET_SETUP == tournament.getStatus()) {
                redirectAttributes.addFlashAttribute(
                        "tournamentNoticeCode", "tournament.host.close.success.bracketSetup");
            } else if (TournamentStatus.CANCELLED == tournament.getStatus()) {
                redirectAttributes.addFlashAttribute(
                        "tournamentNoticeCode", "tournament.host.close.success.cancelled");
            } else {
                redirectAttributes.addFlashAttribute(
                        "tournamentNoticeCode", "tournament.host.close.success");
            }
            return new ModelAndView("redirect:/tournaments/" + tournamentId);
        } catch (final TournamentRegistrationException exception) {
            if (TournamentJoinFailureReason.TOURNAMENT_NOT_FOUND == exception.getReason()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
            if (TournamentJoinFailureReason.FORBIDDEN == exception.getReason()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
            redirectAttributes.addFlashAttribute(
                    "tournamentErrorCode", registrationErrorCode(exception.getReason()));
            return new ModelAndView("redirect:/tournaments/" + tournamentId);
        }
    }

    @PostMapping("/host/tournaments/{tournamentId:\\d+}/bracket/generate")
    public ModelAndView generateBracket(
            @PathVariable("tournamentId") final Long tournamentId,
            final RedirectAttributes redirectAttributes) {
        final User actingUser = SecurityControllerUtils.requireAuthenticatedUser();

        try {
            tournamentBracketService.generateBracket(tournamentId, actingUser);
            redirectAttributes.addFlashAttribute(
                    "tournamentNoticeCode", "tournament.bracket.generate.success");
            return new ModelAndView(
                    "redirect:/host/tournaments/" + tournamentId + "/bracket/setup");
        } catch (final TournamentBracketException exception) {
            handleBracketException(exception, redirectAttributes);
            return new ModelAndView("redirect:/tournaments/" + tournamentId);
        }
    }

    @GetMapping("/host/tournaments/{tournamentId:\\d+}/bracket/setup")
    public ModelAndView showBracketSetup(
            @PathVariable("tournamentId") final Long tournamentId,
            final org.springframework.ui.Model model,
            final Locale locale) {
        final User actingUser = SecurityControllerUtils.requireAuthenticatedUser();
        final Tournament tournament =
                tournamentService
                        .findTournamentForHost(tournamentId, actingUser)
                        .orElseGet(
                                () -> {
                                    if (tournamentService
                                            .findPublicTournament(tournamentId)
                                            .isPresent()) {
                                        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
                                    }
                                    throw new ResponseStatusException(HttpStatus.NOT_FOUND);
                                });

        TournamentBracketViewModel bracketPage;
        try {
            bracketPage =
                    buildBracketPage(
                            tournamentBracketService.getBracket(tournamentId, actingUser), locale);
        } catch (final TournamentBracketException exception) {
            if (TournamentBracketFailureReason.BRACKET_NOT_GENERATED != exception.getReason()) {
                handleBracketException(exception, null);
                throw exception;
            }
            bracketPage = buildUngeneratedBracketPage(tournament, locale);
        }

        final ModelAndView mav = new ModelAndView("host/tournaments/bracket-setup");
        mav.addObject(
                "pageTitle",
                messageSource.getMessage(
                        "page.title.hostTournamentBracketSetup",
                        new Object[] {tournament.getTitle()},
                        locale));
        mav.addObject(
                "shell",
                ShellViewModelFactory.playerShell(
                        messageSource,
                        locale,
                        "/host/tournaments/" + tournamentId + "/bracket/setup"));
        mav.addObject("bracketPage", bracketPage);
        mav.addObject(
                "generateBracketPath", "/host/tournaments/" + tournamentId + "/bracket/generate");
        mav.addObject(
                "publishBracketPath", "/host/tournaments/" + tournamentId + "/bracket/publish");
        mav.addObject("tournamentDetailPath", "/tournaments/" + tournamentId);
        mav.addObject(
                "tournamentNoticeCode", flashString(model, "tournamentNoticeCode").orElse(null));
        mav.addObject(
                "tournamentErrorCode", flashString(model, "tournamentErrorCode").orElse(null));
        return mav;
    }

    @PostMapping("/host/tournaments/{tournamentId:\\d+}/bracket/publish")
    public ModelAndView publishBracket(
            @PathVariable("tournamentId") final Long tournamentId,
            final HttpServletRequest request,
            final RedirectAttributes redirectAttributes) {
        final User actingUser = SecurityControllerUtils.requireAuthenticatedUser();

        try {
            final TournamentBracketView bracketView =
                    tournamentBracketService.getBracket(tournamentId, actingUser);
            final List<TournamentMatchScheduleRequest> schedules =
                    roundOneSchedules(bracketView, request);
            tournamentBracketService.publishBracket(tournamentId, actingUser, schedules);
            redirectAttributes.addFlashAttribute(
                    "tournamentNoticeCode", "tournament.bracket.publish.success");
            return new ModelAndView("redirect:/tournaments/" + tournamentId);
        } catch (final TournamentBracketException exception) {
            handleBracketException(exception, redirectAttributes);
            return new ModelAndView(
                    "redirect:/host/tournaments/" + tournamentId + "/bracket/setup");
        } catch (final IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                    "tournamentErrorCode", "tournament.bracket.error.invalidSchedule");
            return new ModelAndView(
                    "redirect:/host/tournaments/" + tournamentId + "/bracket/setup");
        }
    }

    private ModelAndView createFormView(
            final CreateTournamentForm form, final String formError, final Locale locale) {
        final ModelAndView mav = new ModelAndView("host/tournaments/create");
        mav.addObject(
                "pageTitle",
                messageSource.getMessage("page.title.hostTournamentCreate", null, locale));
        mav.addObject(
                "shell",
                ShellViewModelFactory.playerShell(messageSource, locale, "/host/tournaments/new"));
        mav.addObject("createTournamentForm", form);
        mav.addObject("formError", formError);
        mav.addObject(
                "formTitle", messageSource.getMessage("tournament.create.title", null, locale));
        mav.addObject(
                "formDescription",
                messageSource.getMessage("tournament.create.description", null, locale));
        mav.addObject("formAction", "/host/tournaments");
        mav.addObject(
                "submitLabel",
                messageSource.getMessage("tournament.form.submit.create", null, locale));
        mav.addObject(
                "submitLoadingLabel",
                messageSource.getMessage("tournament.form.submit.creating", null, locale));
        return mav;
    }

    private void applyFormValidation(
            final CreateTournamentForm form,
            final BindingResult bindingResult,
            final Locale locale) {
        validateSport(form, bindingResult, locale);
        validateBracketSize(form, bindingResult, locale);
        validateTeamSize(form, bindingResult, locale);
        validateJoinMode(form, bindingResult, locale);
        validateCoordinates(form, bindingResult, locale);
        validateRegistrationWindow(form, bindingResult, locale);
    }

    private void validateSport(
            final CreateTournamentForm form,
            final BindingResult bindingResult,
            final Locale locale) {
        if (bindingResult.hasFieldErrors("sport")) {
            return;
        }
        if (PersistableEnum.fromDbValue(Sport.class, form.getSport()).isEmpty()) {
            bindingResult.rejectValue(
                    "sport",
                    "CreateTournamentForm.sport.Valid",
                    messageSource.getMessage("CreateTournamentForm.sport.Valid", null, locale));
        }
    }

    private void validateBracketSize(
            final CreateTournamentForm form,
            final BindingResult bindingResult,
            final Locale locale) {
        if (bindingResult.hasFieldErrors("bracketSize")) {
            return;
        }
        if (!SUPPORTED_BRACKET_SIZES.contains(form.getBracketSize())) {
            bindingResult.rejectValue(
                    "bracketSize",
                    "CreateTournamentForm.bracketSize.Valid",
                    messageSource.getMessage(
                            "CreateTournamentForm.bracketSize.Valid", null, locale));
        }
    }

    private void validateTeamSize(
            final CreateTournamentForm form,
            final BindingResult bindingResult,
            final Locale locale) {
        if (bindingResult.hasFieldErrors("sport") || bindingResult.hasFieldErrors("teamSize")) {
            return;
        }
        final Sport sport = PersistableEnum.fromDbValue(Sport.class, form.getSport()).orElse(null);
        if (sport == null) {
            return;
        }
        final List<Integer> supportedTeamSizes =
                SUPPORTED_TEAM_SIZES_BY_SPORT.getOrDefault(sport, List.of());
        if (!supportedTeamSizes.contains(form.getTeamSize())) {
            bindingResult.rejectValue(
                    "teamSize",
                    "CreateTournamentForm.teamSize.ValidForSport",
                    messageSource.getMessage(
                            "CreateTournamentForm.teamSize.ValidForSport", null, locale));
        }
    }

    private void validateJoinMode(
            final CreateTournamentForm form,
            final BindingResult bindingResult,
            final Locale locale) {
        if (!form.isAllowSoloSignup() && !form.isAllowTeamDraft()) {
            bindingResult.rejectValue(
                    "allowSoloSignup",
                    "CreateTournamentForm.joinMode.Required",
                    messageSource.getMessage(
                            "CreateTournamentForm.joinMode.Required", null, locale));
        }
    }

    private void validateCoordinates(
            final CreateTournamentForm form,
            final BindingResult bindingResult,
            final Locale locale) {
        final String latitude = normalizeBlank(form.getLatitude());
        final String longitude = normalizeBlank(form.getLongitude());
        final boolean hasLatitude = !latitude.isEmpty();
        final boolean hasLongitude = !longitude.isEmpty();

        if (hasLatitude != hasLongitude) {
            bindingResult.rejectValue(
                    hasLatitude ? "longitude" : "latitude",
                    "CreateTournamentForm.coordinates.Pair",
                    messageSource.getMessage(
                            "CreateTournamentForm.coordinates.Pair", null, locale));
            return;
        }
        if (!hasLatitude) {
            return;
        }

        final Double parsedLatitude = parseCoordinate(latitude);
        final Double parsedLongitude = parseCoordinate(longitude);
        if (parsedLatitude == null || parsedLatitude < -90 || parsedLatitude > 90) {
            bindingResult.rejectValue(
                    "latitude",
                    "CreateTournamentForm.coordinates.Invalid",
                    messageSource.getMessage(
                            "CreateTournamentForm.coordinates.Invalid", null, locale));
        }
        if (parsedLongitude == null || parsedLongitude < -180 || parsedLongitude > 180) {
            bindingResult.rejectValue(
                    "longitude",
                    "CreateTournamentForm.coordinates.Invalid",
                    messageSource.getMessage(
                            "CreateTournamentForm.coordinates.Invalid", null, locale));
        }
    }

    private void validateRegistrationWindow(
            final CreateTournamentForm form,
            final BindingResult bindingResult,
            final Locale locale) {
        if (bindingResult.hasFieldErrors("registrationOpensDate")
                || bindingResult.hasFieldErrors("registrationOpensTime")
                || bindingResult.hasFieldErrors("registrationClosesDate")
                || bindingResult.hasFieldErrors("registrationClosesTime")) {
            return;
        }

        final Instant opensAt =
                toInstant(
                        form.getRegistrationOpensDate(),
                        form.getRegistrationOpensTime(),
                        form.getTz());
        final Instant closesAt =
                toInstant(
                        form.getRegistrationClosesDate(),
                        form.getRegistrationClosesTime(),
                        form.getTz());

        if (!closesAt.isAfter(opensAt)) {
            bindingResult.rejectValue(
                    "registrationClosesTime",
                    "CreateTournamentForm.registrationClosesTime.AfterOpen",
                    messageSource.getMessage(
                            "CreateTournamentForm.registrationClosesTime.AfterOpen", null, locale));
            return;
        }
        if (!closesAt.isAfter(Instant.now(clock))) {
            bindingResult.rejectValue(
                    "registrationClosesTime",
                    "CreateTournamentForm.registrationClosesTime.Future",
                    messageSource.getMessage(
                            "CreateTournamentForm.registrationClosesTime.Future", null, locale));
        }
    }

    private void applyServiceError(
            final TournamentLifecycleException exception,
            final BindingResult bindingResult,
            final Locale locale) {
        final String code = lifecycleErrorCode(exception.getReason());
        final String message = messageSource.getMessage(code, null, exception.getMessage(), locale);
        switch (exception.getReason()) {
            case INVALID_BRACKET_SIZE:
                bindingResult.rejectValue("bracketSize", code, message);
                break;
            case INVALID_TEAM_SIZE:
                bindingResult.rejectValue("teamSize", code, message);
                break;
            case INVALID_REGISTRATION_WINDOW:
                bindingResult.rejectValue("registrationClosesTime", code, message);
                break;
            case INVALID_SCHEDULE:
                bindingResult.reject("CreateTournamentForm.global", message);
                break;
            case INVALID_JOIN_MODE:
                bindingResult.rejectValue("allowSoloSignup", code, message);
                break;
            case INVALID_FORMAT:
            case INVALID_DETAILS:
            case NOT_EDITABLE:
            case NOT_CANCELLABLE:
            case ALREADY_COMPLETED:
            case FORBIDDEN:
            case TOURNAMENT_NOT_FOUND:
            default:
                bindingResult.reject("CreateTournamentForm.global", message);
                break;
        }
    }

    private static String lifecycleErrorCode(final TournamentLifecycleFailureReason reason) {
        switch (reason) {
            case INVALID_BRACKET_SIZE:
                return "tournament.lifecycle.error.invalidBracketSize";
            case INVALID_TEAM_SIZE:
                return "tournament.lifecycle.error.invalidTeamSize";
            case INVALID_JOIN_MODE:
                return "tournament.lifecycle.error.invalidJoinMode";
            case INVALID_REGISTRATION_WINDOW:
                return "tournament.lifecycle.error.invalidRegistrationWindow";
            case INVALID_SCHEDULE:
                return "tournament.lifecycle.error.invalidSchedule";
            case INVALID_FORMAT:
                return "tournament.lifecycle.error.invalidFormat";
            case FORBIDDEN:
                return "tournament.lifecycle.error.forbidden";
            case TOURNAMENT_NOT_FOUND:
                return "tournament.lifecycle.error.notFound";
            case NOT_EDITABLE:
                return "tournament.lifecycle.error.notEditable";
            case NOT_CANCELLABLE:
                return "tournament.lifecycle.error.notCancellable";
            case ALREADY_COMPLETED:
                return "tournament.lifecycle.error.alreadyCompleted";
            default:
                return "tournament.lifecycle.error.invalidDetails";
        }
    }

    private static String registrationErrorCode(final TournamentJoinFailureReason reason) {
        switch (reason) {
            case SOLO_SIGNUP_DISABLED:
                return "tournament.registration.error.soloDisabled";
            case REGISTRATION_NOT_OPEN:
                return "tournament.registration.error.notOpen";
            case ALREADY_ON_TEAM:
                return "tournament.registration.error.alreadyOnTeam";
            case ALREADY_ASSIGNED:
                return "tournament.registration.error.alreadyAssigned";
            case NOT_IN_SOLO_POOL:
                return "tournament.registration.error.notInSoloPool";
            case ALREADY_IN_SOLO_POOL:
                return "tournament.registration.error.alreadyInSoloPool";
            case FORBIDDEN:
                return "tournament.registration.error.forbidden";
            case TOURNAMENT_NOT_FOUND:
            default:
                return "tournament.registration.error.notFound";
        }
    }

    private void handleBracketException(
            final TournamentBracketException exception,
            final RedirectAttributes redirectAttributes) {
        if (TournamentBracketFailureReason.TOURNAMENT_NOT_FOUND == exception.getReason()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (TournamentBracketFailureReason.FORBIDDEN == exception.getReason()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (redirectAttributes != null) {
            redirectAttributes.addFlashAttribute(
                    "tournamentErrorCode", bracketErrorCode(exception.getReason()));
        }
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
            case MISSING_ROUND_ONE_SCHEDULE:
                return "tournament.bracket.error.missingRoundOneSchedule";
            case INVALID_SCHEDULE:
                return "tournament.bracket.error.invalidSchedule";
            case NOT_IN_PROGRESS:
                return "tournament.bracket.error.notInProgress";
            case MATCH_NOT_READY:
                return "tournament.bracket.error.matchNotReady";
            case MATCH_ALREADY_DECIDED:
                return "tournament.bracket.error.matchAlreadyDecided";
            case WINNER_NOT_IN_MATCH:
                return "tournament.bracket.error.winnerNotInMatch";
            case FORFEITING_TEAM_NOT_IN_MATCH:
                return "tournament.bracket.error.forfeitingTeamNotInMatch";
            case FORBIDDEN:
                return "tournament.bracket.error.forbidden";
            case TOURNAMENT_NOT_FOUND:
            case TEAM_NOT_FOUND:
            default:
                return "tournament.bracket.error.notFound";
        }
    }

    private TournamentBracketViewModel buildUngeneratedBracketPage(
            final Tournament tournament, final Locale locale) {
        return new TournamentBracketViewModel(
                tournament.getId(),
                tournament.getTitle(),
                statusLabel(tournament, locale),
                statusTone(tournament),
                null,
                null,
                null,
                null,
                false,
                false,
                false,
                List.of());
    }

    private TournamentBracketViewModel buildBracketPage(
            final TournamentBracketView bracketView, final Locale locale) {
        final Tournament tournament = bracketView.getTournament();
        final Long focusedMatchId =
                bracketView.getFocusedMatch() == null
                        ? null
                        : bracketView.getFocusedMatch().getId();
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
                                                                                false,
                                                                                false,
                                                                                false,
                                                                                matchScheduleLabel(
                                                                                        match,
                                                                                        locale),
                                                                                scheduleDate(
                                                                                        scheduleStart(
                                                                                                tournament,
                                                                                                match)),
                                                                                scheduleTime(
                                                                                        scheduleStart(
                                                                                                tournament,
                                                                                                match)),
                                                                                scheduleDate(
                                                                                        scheduleEnd(
                                                                                                tournament,
                                                                                                match)),
                                                                                scheduleTime(
                                                                                        scheduleEnd(
                                                                                                tournament,
                                                                                                match)),
                                                                                scheduleAddress(
                                                                                        tournament,
                                                                                        match),
                                                                                scheduleLatitude(
                                                                                        tournament,
                                                                                        match),
                                                                                scheduleLongitude(
                                                                                        tournament,
                                                                                        match)))
                                                        .toList()))
                        .toList();
        return new TournamentBracketViewModel(
                tournament.getId(),
                tournament.getTitle(),
                statusLabel(tournament, locale),
                statusTone(tournament),
                focusedMatchLabel(bracketView.getFocusedMatch(), locale),
                focusedMatchTeamsLabel(bracketView.getFocusedMatch(), locale),
                matchScheduleLabel(bracketView.getFocusedMatch(), locale),
                bracketView.getFocusedMatch() == null
                        ? null
                        : bracketView.getFocusedMatch().getAddress(),
                true,
                TournamentStatus.BRACKET_SETUP == tournament.getStatus(),
                false,
                rounds);
    }

    private String statusLabel(final Tournament tournament, final Locale locale) {
        return messageSource.getMessage(
                "tournament.status." + tournament.getStatus().getDbValue(), null, locale);
    }

    private static String statusTone(final Tournament tournament) {
        return tournament.getStatus().getDbValue().replace('_', '-');
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

    private String teamName(final ar.edu.itba.paw.models.TournamentTeam team, final Locale locale) {
        return team == null
                ? messageSource.getMessage("tournament.bracket.team.tbd", null, locale)
                : team.getName();
    }

    private static Long teamId(final ar.edu.itba.paw.models.TournamentTeam team) {
        return team == null ? null : team.getId();
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

    private List<TournamentMatchScheduleRequest> roundOneSchedules(
            final TournamentBracketView bracketView, final HttpServletRequest request) {
        return bracketView.getMatches().stream()
                .filter(match -> match.getRoundNumber() == 1)
                .sorted(Comparator.comparingInt(TournamentMatch::getMatchIndex))
                .map(match -> scheduleRequest(match, request))
                .toList();
    }

    private TournamentMatchScheduleRequest scheduleRequest(
            final TournamentMatch match, final HttpServletRequest request) {
        final long matchId = Objects.requireNonNull(match.getId());
        return new TournamentMatchScheduleRequest(
                matchId,
                scheduleInstant(request, "start", matchId),
                scheduleInstant(request, "end", matchId),
                requiredParam(request, "address_" + matchId),
                parseCoordinate(request.getParameter("latitude_" + matchId)),
                parseCoordinate(request.getParameter("longitude_" + matchId)));
    }

    private Instant scheduleInstant(
            final HttpServletRequest request, final String prefix, final long matchId) {
        final LocalDate date = LocalDate.parse(requiredParam(request, prefix + "Date_" + matchId));
        final LocalTime time = LocalTime.parse(requiredParam(request, prefix + "Time_" + matchId));
        return toInstant(date, time, request.getParameter("tz"));
    }

    private static String requiredParam(final HttpServletRequest request, final String name) {
        final String value = normalizeBlank(request.getParameter(name));
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Missing " + name);
        }
        return value;
    }

    private static Instant scheduleStart(final Tournament tournament, final TournamentMatch match) {
        return match.getScheduledStartsAt() == null
                ? tournament.getStartsAt()
                : match.getScheduledStartsAt();
    }

    private static Instant scheduleEnd(final Tournament tournament, final TournamentMatch match) {
        return match.getScheduledEndsAt() == null
                ? tournament.getEndsAt()
                : match.getScheduledEndsAt();
    }

    private static String scheduleDate(final Instant instant) {
        return instant == null
                ? ""
                : LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).toLocalDate().toString();
    }

    private static String scheduleTime(final Instant instant) {
        return instant == null
                ? ""
                : TIME_INPUT_FORMATTER.format(
                        LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).toLocalTime());
    }

    private static String scheduleAddress(
            final Tournament tournament, final TournamentMatch match) {
        return normalizeBlank(match.getAddress()).isEmpty()
                ? tournament.getAddress()
                : match.getAddress();
    }

    private static String scheduleLatitude(
            final Tournament tournament, final TournamentMatch match) {
        final Double latitude =
                match.getLatitude() == null ? tournament.getLatitude() : match.getLatitude();
        return latitude == null ? "" : latitude.toString();
    }

    private static String scheduleLongitude(
            final Tournament tournament, final TournamentMatch match) {
        final Double longitude =
                match.getLongitude() == null ? tournament.getLongitude() : match.getLongitude();
        return longitude == null ? "" : longitude.toString();
    }

    private static java.util.Optional<String> flashString(
            final org.springframework.ui.Model model, final String name) {
        final Object value = model.asMap().get(name);
        return value instanceof String
                ? java.util.Optional.of((String) value)
                : java.util.Optional.empty();
    }

    private static Instant toInstant(
            final java.time.LocalDate date, final java.time.LocalTime time, final String timezone) {
        return date.atTime(time).atZone(resolveZoneId(timezone)).toInstant();
    }

    private static ZoneId resolveZoneId(final String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return ZoneId.systemDefault();
        }
        try {
            return ZoneId.of(timezone);
        } catch (final Exception ignored) {
            return ZoneId.systemDefault();
        }
    }

    private static Double parseCoordinate(final String value) {
        final String normalized = normalizeBlank(value);
        if (normalized.isEmpty()) {
            return null;
        }
        try {
            return Double.valueOf(normalized);
        } catch (final NumberFormatException exception) {
            return null;
        }
    }

    private static String normalizeBlank(final String value) {
        return value == null ? "" : value.trim();
    }
}
