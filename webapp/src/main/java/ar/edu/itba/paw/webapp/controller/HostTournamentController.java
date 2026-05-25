package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.types.PersistableEnum;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.models.types.TournamentFormat;
import ar.edu.itba.paw.models.types.TournamentStatus;
import ar.edu.itba.paw.services.CreateTournamentRequest;
import ar.edu.itba.paw.services.TournamentJoinFailureReason;
import ar.edu.itba.paw.services.TournamentLifecycleFailureReason;
import ar.edu.itba.paw.services.TournamentRegistrationService;
import ar.edu.itba.paw.services.TournamentService;
import ar.edu.itba.paw.services.exceptions.TournamentLifecycleException;
import ar.edu.itba.paw.services.exceptions.TournamentRegistrationException;
import ar.edu.itba.paw.webapp.form.CreateTournamentForm;
import ar.edu.itba.paw.webapp.utils.SecurityControllerUtils;
import ar.edu.itba.paw.webapp.viewmodel.ShellViewModelFactory;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

    private final TournamentService tournamentService;
    private final TournamentRegistrationService tournamentRegistrationService;
    private final MessageSource messageSource;
    private final Clock clock;

    public HostTournamentController(
            final TournamentService tournamentService,
            final TournamentRegistrationService tournamentRegistrationService,
            final MessageSource messageSource,
            final Clock clock) {
        this.tournamentService = tournamentService;
        this.tournamentRegistrationService = tournamentRegistrationService;
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
                        false,
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
        if (!form.isAllowSoloSignup()) {
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
            case INVALID_JOIN_MODE:
                bindingResult.rejectValue("allowSoloSignup", code, message);
                break;
            case INVALID_SCHEDULE:
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

    private static Instant toInstant(
            final LocalDate date, final LocalTime time, final String timezone) {
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
