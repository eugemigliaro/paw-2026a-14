package ar.edu.itba.paw.webapp.controller;

import static ar.edu.itba.paw.webapp.utils.ViewFormatUtils.formatInstant;

import ar.edu.itba.paw.models.ImageMetadata;
import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.TournamentMatch;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.types.PersistableEnum;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.models.types.TournamentFormat;
import ar.edu.itba.paw.models.types.TournamentPairingStrategy;
import ar.edu.itba.paw.models.types.TournamentStatus;
import ar.edu.itba.paw.models.types.TournamentTeamOrigin;
import ar.edu.itba.paw.services.CreateTournamentRequest;
import ar.edu.itba.paw.services.ImageService;
import ar.edu.itba.paw.services.TournamentBracketFailureReason;
import ar.edu.itba.paw.services.TournamentBracketService;
import ar.edu.itba.paw.services.TournamentBracketView;
import ar.edu.itba.paw.services.TournamentJoinFailureReason;
import ar.edu.itba.paw.services.TournamentLifecycleFailureReason;
import ar.edu.itba.paw.services.TournamentMatchScheduleRequest;
import ar.edu.itba.paw.services.TournamentRegistrationService;
import ar.edu.itba.paw.services.TournamentService;
import ar.edu.itba.paw.services.UpdateTournamentRequest;
import ar.edu.itba.paw.services.exceptions.TournamentBracketException;
import ar.edu.itba.paw.services.exceptions.TournamentLifecycleException;
import ar.edu.itba.paw.services.exceptions.TournamentRegistrationException;
import ar.edu.itba.paw.webapp.form.CreateTournamentForm;
import ar.edu.itba.paw.webapp.utils.ImageUrlHelper;
import ar.edu.itba.paw.webapp.utils.SecurityControllerUtils;
import ar.edu.itba.paw.webapp.viewmodel.ShellViewModelFactory;
import ar.edu.itba.paw.webapp.viewmodel.TournamentBracketViewModel;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("isAuthenticated()")
public class HostTournamentController {

    private static final double DEFAULT_MAP_LATITUDE = -34.6037;
    private static final double DEFAULT_MAP_LONGITUDE = -58.3816;
    private static final int DEFAULT_MAP_ZOOM = 14;

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
    private final ImageService imageService;
    private final MessageSource messageSource;
    private final Clock clock;
    private final boolean mapPickerEnabled;
    private final String mapTileUrlTemplate;
    private final String mapAttribution;
    private final double mapDefaultLatitude;
    private final double mapDefaultLongitude;
    private final int mapDefaultZoom;

    @org.springframework.beans.factory.annotation.Autowired
    public HostTournamentController(
            final TournamentService tournamentService,
            final TournamentRegistrationService tournamentRegistrationService,
            final TournamentBracketService tournamentBracketService,
            final ImageService imageService,
            final MessageSource messageSource,
            final Clock clock,
            @Value("${map.picker.enabled:false}") final boolean mapPickerEnabled,
            @Value("${map.tiles.urlTemplate:}") final String mapTileUrlTemplate,
            @Value("${map.tiles.attribution:}") final String mapAttribution,
            @Value("${map.default.latitude:" + DEFAULT_MAP_LATITUDE + "}")
                    final double mapDefaultLatitude,
            @Value("${map.default.longitude:" + DEFAULT_MAP_LONGITUDE + "}")
                    final double mapDefaultLongitude,
            @Value("${map.default.zoom:" + DEFAULT_MAP_ZOOM + "}") final int mapDefaultZoom) {
        this.tournamentService = tournamentService;
        this.tournamentRegistrationService = tournamentRegistrationService;
        this.tournamentBracketService = tournamentBracketService;
        this.imageService = imageService;
        this.messageSource = messageSource;
        this.clock = clock;
        this.mapPickerEnabled = mapPickerEnabled;
        this.mapTileUrlTemplate = mapTileUrlTemplate == null ? "" : mapTileUrlTemplate;
        this.mapAttribution = mapAttribution == null ? "" : mapAttribution;
        this.mapDefaultLatitude = mapDefaultLatitude;
        this.mapDefaultLongitude = mapDefaultLongitude;
        this.mapDefaultZoom = mapDefaultZoom;
    }

    public HostTournamentController(
            final TournamentService tournamentService,
            final TournamentRegistrationService tournamentRegistrationService,
            final TournamentBracketService tournamentBracketService,
            final MessageSource messageSource,
            final Clock clock,
            final boolean mapPickerEnabled,
            final String mapTileUrlTemplate,
            final String mapAttribution,
            final double mapDefaultLatitude,
            final double mapDefaultLongitude,
            final int mapDefaultZoom) {
        this(
                tournamentService,
                tournamentRegistrationService,
                tournamentBracketService,
                null,
                messageSource,
                clock,
                mapPickerEnabled,
                mapTileUrlTemplate,
                mapAttribution,
                mapDefaultLatitude,
                mapDefaultLongitude,
                mapDefaultZoom);
    }

    public HostTournamentController(
            final TournamentService tournamentService,
            final TournamentRegistrationService tournamentRegistrationService,
            final TournamentBracketService tournamentBracketService,
            final MessageSource messageSource,
            final Clock clock) {
        this(
                tournamentService,
                tournamentRegistrationService,
                tournamentBracketService,
                null,
                messageSource,
                clock,
                false,
                "",
                "",
                DEFAULT_MAP_LATITUDE,
                DEFAULT_MAP_LONGITUDE,
                DEFAULT_MAP_ZOOM);
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

        applyFormValidation(createTournamentForm, bindingResult, locale, true);

        if (bindingResult.hasErrors()) {
            return createFormView(createTournamentForm, null, locale, createFormConfig(locale));
        }

        final ImageMetadata bannerImageMetadata;
        try {
            bannerImageMetadata = storeBannerIfPresent(createTournamentForm, null);
        } catch (final IllegalArgumentException exception) {
            return createFormView(
                    createTournamentForm, exception.getMessage(), locale, createFormConfig(locale));
        } catch (final IOException exception) {
            return createFormView(
                    createTournamentForm,
                    messageSource.getMessage("host.imageError", null, locale),
                    locale,
                    createFormConfig(locale));
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
                        bannerImageMetadata,
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
            return createFormView(createTournamentForm, null, locale, createFormConfig(locale));
        } catch (final IllegalArgumentException exception) {
            return createFormView(
                    createTournamentForm, exception.getMessage(), locale, createFormConfig(locale));
        }
    }

    @GetMapping("/host/tournaments/{tournamentId:\\d+}/edit")
    public ModelAndView showEditTournament(
            @PathVariable("tournamentId") final Long tournamentId, final Locale locale) {
        final User actingUser = SecurityControllerUtils.requireAuthenticatedUser();
        final Tournament tournament =
                tournamentService
                        .findTournamentForHost(tournamentId, actingUser)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!isEditable(tournament)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return createFormView(toForm(tournament), null, locale, editFormConfig(tournament, locale));
    }

    @PostMapping("/host/tournaments/{tournamentId:\\d+}/edit")
    public ModelAndView updateTournament(
            @PathVariable("tournamentId") final Long tournamentId,
            @Valid @ModelAttribute("createTournamentForm")
                    final CreateTournamentForm createTournamentForm,
            final BindingResult bindingResult,
            final Locale locale,
            final RedirectAttributes redirectAttributes) {
        final User actingUser = SecurityControllerUtils.requireAuthenticatedUser();
        final Tournament tournament =
                tournamentService
                        .findTournamentForHost(tournamentId, actingUser)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        final TournamentFormConfig formConfig = editFormConfig(tournament, locale);

        applyFormValidation(createTournamentForm, bindingResult, locale, false);

        if (bindingResult.hasErrors()) {
            return createFormView(createTournamentForm, null, locale, formConfig);
        }

        final ImageMetadata bannerImageMetadata;
        try {
            bannerImageMetadata =
                    storeBannerIfPresent(createTournamentForm, tournament.getBannerImageMetadata());
        } catch (final IllegalArgumentException exception) {
            return createFormView(createTournamentForm, exception.getMessage(), locale, formConfig);
        } catch (final IOException exception) {
            return createFormView(
                    createTournamentForm,
                    messageSource.getMessage("host.imageError", null, locale),
                    locale,
                    formConfig);
        }

        final Sport sport =
                PersistableEnum.fromDbValue(Sport.class, createTournamentForm.getSport())
                        .orElse(Sport.PADEL);
        final UpdateTournamentRequest request =
                new UpdateTournamentRequest(
                        sport,
                        createTournamentForm.getTitle(),
                        createTournamentForm.getDescription(),
                        createTournamentForm.getAddress(),
                        parseCoordinate(createTournamentForm.getLatitude()),
                        parseCoordinate(createTournamentForm.getLongitude()),
                        tournament.getStartsAt(),
                        tournament.getEndsAt(),
                        createTournamentForm.getPricePerPlayer(),
                        bannerImageMetadata,
                        createTournamentForm.getBracketSize(),
                        createTournamentForm.getTeamSize(),
                        toInstant(
                                createTournamentForm.getRegistrationOpensDate(),
                                createTournamentForm.getRegistrationOpensTime(),
                                createTournamentForm.getTz()),
                        toInstant(
                                createTournamentForm.getRegistrationClosesDate(),
                                createTournamentForm.getRegistrationClosesTime(),
                                createTournamentForm.getTz()));

        try {
            tournamentService.update(tournamentId, actingUser, request);
        } catch (final TournamentLifecycleException exception) {
            if (TournamentLifecycleFailureReason.TOURNAMENT_NOT_FOUND == exception.getReason()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
            if (TournamentLifecycleFailureReason.FORBIDDEN == exception.getReason()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
            applyServiceError(exception, bindingResult, locale);
            return createFormView(createTournamentForm, null, locale, formConfig);
        }

        redirectAttributes.addFlashAttribute(
                "tournamentNoticeCode", "tournament.host.edit.success");
        return new ModelAndView("redirect:/tournaments/" + tournamentId);
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

    @PostMapping("/host/tournaments/{tournamentId:\\d+}/bracket/strategy")
    public ModelAndView updateBracketStrategy(
            @PathVariable("tournamentId") final Long tournamentId,
            @RequestParam("pairingStrategy") final String pairingStrategyValue,
            final RedirectAttributes redirectAttributes) {
        final User actingUser = SecurityControllerUtils.requireAuthenticatedUser();
        final TournamentPairingStrategy pairingStrategy =
                PersistableEnum.fromDbValue(TournamentPairingStrategy.class, pairingStrategyValue)
                        .orElse(null);
        try {
            tournamentBracketService.updatePairingStrategy(
                    tournamentId, actingUser, pairingStrategy);
            redirectAttributes.addFlashAttribute(
                    "tournamentNoticeCode", "tournament.bracket.strategy.updated");
        } catch (final TournamentBracketException exception) {
            handleBracketException(exception, redirectAttributes);
        }
        return new ModelAndView("redirect:/host/tournaments/" + tournamentId + "/bracket/setup");
    }

    @PostMapping("/host/tournaments/{tournamentId:\\d+}/cancel")
    public ModelAndView cancelTournament(
            @PathVariable("tournamentId") final Long tournamentId,
            final RedirectAttributes redirectAttributes) {
        final User actingUser = SecurityControllerUtils.requireAuthenticatedUser();

        try {
            tournamentService.cancel(tournamentId, actingUser, "Host cancelled tournament");
            redirectAttributes.addFlashAttribute(
                    "tournamentNoticeCode", "tournament.host.cancel.success");
            return new ModelAndView("redirect:/tournaments/" + tournamentId);
        } catch (final TournamentLifecycleException exception) {
            if (TournamentLifecycleFailureReason.TOURNAMENT_NOT_FOUND == exception.getReason()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
            if (TournamentLifecycleFailureReason.FORBIDDEN == exception.getReason()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
            redirectAttributes.addFlashAttribute(
                    "tournamentErrorCode", lifecycleErrorCode(exception.getReason()));
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
            return new ModelAndView(
                    "redirect:/host/tournaments/" + tournamentId + "/bracket/setup");
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
                "selectedPairingStrategy",
                tournament.getPairingStrategy() == null
                        ? TournamentPairingStrategy.RANDOM.getDbValue()
                        : tournament.getPairingStrategy().getDbValue());
        mav.addObject(
                "generateBracketPath", "/host/tournaments/" + tournamentId + "/bracket/generate");
        mav.addObject(
                "updateBracketStrategyPath",
                "/host/tournaments/" + tournamentId + "/bracket/strategy");
        mav.addObject(
                "saveManualPairingsPath",
                "/host/tournaments/" + tournamentId + "/bracket/manual-pairings");
        mav.addObject(
                "manualPairingEnabled",
                TournamentPairingStrategy.MANUAL == tournament.getPairingStrategy());
        mav.addObject("tournamentPairingStrategy", tournament.getPairingStrategy());
        if (!bracketPage.isGenerated()
                && TournamentPairingStrategy.MANUAL == tournament.getPairingStrategy()) {
            mav.addObject(
                    "manualPairingTeams",
                    tournamentBracketService.listTeamsForSetup(tournamentId, actingUser));
        } else {
            mav.addObject("manualPairingTeams", List.of());
        }
        mav.addObject(
                "publishBracketPath", "/host/tournaments/" + tournamentId + "/bracket/publish");
        mav.addObject("tournamentDetailPath", "/tournaments/" + tournamentId);
        mav.addObject(
                "tournamentNoticeCode", flashString(model, "tournamentNoticeCode").orElse(null));
        mav.addObject(
                "tournamentErrorCode", flashString(model, "tournamentErrorCode").orElse(null));
        return mav;
    }

    @PostMapping("/host/tournaments/{tournamentId:\\d+}/bracket/manual-pairings")
    public ModelAndView saveManualPairings(
            @PathVariable("tournamentId") final Long tournamentId,
            final HttpServletRequest request,
            final RedirectAttributes redirectAttributes) {
        final User actingUser = SecurityControllerUtils.requireAuthenticatedUser();
        final String[] teamIdValues = request.getParameterValues("teamIds");
        final List<Long> orderedTeamIds = new ArrayList<>();
        if (teamIdValues != null) {
            for (final String teamIdValue : teamIdValues) {
                orderedTeamIds.add(Long.parseLong(teamIdValue));
            }
        }
        try {
            tournamentBracketService.saveManualPairings(tournamentId, actingUser, orderedTeamIds);
            redirectAttributes.addFlashAttribute(
                    "tournamentNoticeCode", "tournament.bracket.manualPairings.saved");
        } catch (final TournamentBracketException exception) {
            handleBracketException(exception, redirectAttributes);
        } catch (final IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                    "tournamentErrorCode", "tournament.bracket.error.invalidPairings");
        }
        return new ModelAndView("redirect:/host/tournaments/" + tournamentId + "/bracket/setup");
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
                    allMatchSchedules(bracketView, request);
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
            final CreateTournamentForm form,
            final String formError,
            final Locale locale,
            final TournamentFormConfig formConfig) {
        final ModelAndView mav = new ModelAndView("host/tournaments/create");
        mav.addObject("pageTitle", formConfig.pageTitle());
        mav.addObject(
                "shell",
                ShellViewModelFactory.playerShell(messageSource, locale, "/host/tournaments/new"));
        mav.addObject("createTournamentForm", form);
        mav.addObject("formError", formError);
        mav.addObject("formTitle", formConfig.title());
        mav.addObject("formDescription", formConfig.description());
        mav.addObject("formAction", formConfig.action());
        mav.addObject("submitLabel", formConfig.submitLabel());
        mav.addObject("submitLoadingLabel", formConfig.submitLoadingLabel());
        mav.addObject("isEditMode", formConfig.editMode());
        mav.addObject("currentBannerImageUrl", formConfig.bannerImageUrl());
        mav.addObject("mapPickerEnabled", mapPickerEnabled && !mapTileUrlTemplate.isBlank());
        mav.addObject("mapTileUrlTemplate", mapTileUrlTemplate);
        mav.addObject("mapAttribution", mapAttribution);
        mav.addObject("mapDefaultLatitude", mapDefaultLatitude);
        mav.addObject("mapDefaultLongitude", mapDefaultLongitude);
        mav.addObject("mapDefaultZoom", mapDefaultZoom);
        return mav;
    }

    private ModelAndView createFormView(
            final CreateTournamentForm form, final String formError, final Locale locale) {
        return createFormView(form, formError, locale, createFormConfig(locale));
    }

    private TournamentFormConfig createFormConfig(final Locale locale) {
        return new TournamentFormConfig(
                messageSource.getMessage("page.title.hostTournamentCreate", null, locale),
                messageSource.getMessage("tournament.create.title", null, locale),
                messageSource.getMessage("tournament.create.description", null, locale),
                "/host/tournaments",
                messageSource.getMessage("tournament.form.submit.create", null, locale),
                messageSource.getMessage("tournament.form.submit.creating", null, locale),
                false,
                null);
    }

    private TournamentFormConfig editFormConfig(final Tournament tournament, final Locale locale) {
        return new TournamentFormConfig(
                messageSource.getMessage(
                        "page.title.hostTournamentEdit",
                        new Object[] {tournament.getTitle()},
                        locale),
                messageSource.getMessage("tournament.edit.title", null, locale),
                messageSource.getMessage("tournament.edit.description", null, locale),
                "/host/tournaments/" + tournament.getId() + "/edit",
                messageSource.getMessage("tournament.form.submit.edit", null, locale),
                messageSource.getMessage("tournament.form.submit.saving", null, locale),
                true,
                ImageUrlHelper.bannerUrlFor(tournament));
    }

    private ImageMetadata storeBannerIfPresent(
            final CreateTournamentForm form, final ImageMetadata fallbackBannerImage)
            throws IOException {
        if (form.getBannerImage() == null || form.getBannerImage().isEmpty()) {
            return fallbackBannerImage;
        }
        if (imageService == null) {
            throw new IllegalArgumentException(
                    messageSource.getMessage("host.imageError", null, Locale.getDefault()));
        }

        Long imageId;
        try (InputStream inputStream = form.getBannerImage().getInputStream()) {
            imageId =
                    imageService.store(
                            form.getBannerImage().getContentType(),
                            form.getBannerImage().getSize(),
                            inputStream);
        }
        return new ImageMetadata(
                imageId, form.getBannerImage().getContentType(), form.getBannerImage().getSize());
    }

    private void applyFormValidation(
            final CreateTournamentForm form,
            final BindingResult bindingResult,
            final Locale locale,
            final boolean requireFutureRegistrationClose) {
        validateSport(form, bindingResult, locale);
        validateBracketSize(form, bindingResult, locale);
        validateTeamSize(form, bindingResult, locale);
        validateJoinMode(form, bindingResult, locale);
        validateCoordinates(form, bindingResult, locale);
        validateRegistrationWindow(form, bindingResult, locale, requireFutureRegistrationClose);
    }

    private CreateTournamentForm toForm(final Tournament tournament) {
        final ZoneId zoneId = ZoneId.systemDefault();
        final CreateTournamentForm form = new CreateTournamentForm();
        form.setTitle(tournament.getTitle());
        form.setDescription(tournament.getDescription());
        form.setAddress(tournament.getAddress());
        form.setLatitude(
                tournament.getLatitude() == null ? "" : tournament.getLatitude().toString());
        form.setLongitude(
                tournament.getLongitude() == null ? "" : tournament.getLongitude().toString());
        form.setSport(tournament.getSport().getDbValue());
        form.setBracketSize(tournament.getBracketSize());
        form.setTeamSize(tournament.getTeamSize());
        form.setAllowSoloSignup(tournament.isAllowSoloSignup());
        form.setAllowTeamDraft(tournament.isAllowTeamDraft());
        form.setPricePerPlayer(tournament.getPricePerPlayer());
        form.setRegistrationOpensDate(
                LocalDate.ofInstant(tournament.getRegistrationOpensAt(), zoneId));
        form.setRegistrationOpensTime(
                LocalTime.ofInstant(tournament.getRegistrationOpensAt(), zoneId));
        form.setRegistrationClosesDate(
                LocalDate.ofInstant(tournament.getRegistrationClosesAt(), zoneId));
        form.setRegistrationClosesTime(
                LocalTime.ofInstant(tournament.getRegistrationClosesAt(), zoneId));
        form.setTz(zoneId.getId());
        return form;
    }

    private boolean isEditable(final Tournament tournament) {
        return TournamentStatus.REGISTRATION == tournament.getStatus();
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
            final Locale locale,
            final boolean requireFutureRegistrationClose) {
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
        if (requireFutureRegistrationClose && !closesAt.isAfter(Instant.now(clock))) {
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
            case UNDER_CAPACITY:
                return "tournament.registration.error.underCapacity";
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
            case PAIRING_STRATEGY_REQUIRED:
                return "tournament.bracket.error.pairingStrategyRequired";
            case INVALID_PAIRINGS:
                return "tournament.bracket.error.invalidPairings";
            case MANUAL_PAIRINGS_REQUIRED:
                return "tournament.bracket.error.manualPairingsRequired";
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

    private TournamentBracketViewModel buildUngeneratedBracketPage(
            final Tournament tournament, final Locale locale) {
        return new TournamentBracketViewModel(
                tournament.getId(),
                tournament.getTitle(),
                statusLabel(tournament, locale),
                statusTone(tournament),
                false,
                false,
                false,
                List.of());
    }

    private TournamentBracketViewModel buildBracketPage(
            final TournamentBracketView bracketView, final Locale locale) {
        final Tournament tournament = bracketView.getTournament();
        final String defaultScheduleDate = LocalDate.now(ZoneId.systemDefault()).toString();
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
        final int totalRounds = matchesByRound.size();
        final List<TournamentBracketViewModel.RoundViewModel> rounds =
                matchesByRound.entrySet().stream()
                        .map(
                                entry ->
                                        new TournamentBracketViewModel.RoundViewModel(
                                                entry.getKey(),
                                                roundLabel(entry.getKey(), totalRounds, locale),
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
                                                                                match.getStatus()
                                                                                        .getDbValue()
                                                                                        .replace(
                                                                                                '_',
                                                                                                '-'),
                                                                                false,
                                                                                false,
                                                                                isWinner(
                                                                                        match
                                                                                                .getTeamA(),
                                                                                        match
                                                                                                .getWinnerTeam()),
                                                                                isWinner(
                                                                                        match
                                                                                                .getTeamB(),
                                                                                        match
                                                                                                .getWinnerTeam()),
                                                                                entry.getKey()
                                                                                        == totalRounds,
                                                                                false,
                                                                                matchScheduleLabel(
                                                                                        match,
                                                                                        locale),
                                                                                match
                                                                                                        .getScheduledStartsAt()
                                                                                                == null
                                                                                        ? defaultScheduleDate
                                                                                        : scheduleDate(
                                                                                                match
                                                                                                        .getScheduledStartsAt()),
                                                                                match
                                                                                                        .getScheduledStartsAt()
                                                                                                == null
                                                                                        ? defaultScheduleStartTime(
                                                                                                entry
                                                                                                        .getKey())
                                                                                        : scheduleTime(
                                                                                                match
                                                                                                        .getScheduledStartsAt()),
                                                                                match
                                                                                                        .getScheduledEndsAt()
                                                                                                == null
                                                                                        ? defaultScheduleDate
                                                                                        : scheduleDate(
                                                                                                match
                                                                                                        .getScheduledEndsAt()),
                                                                                match
                                                                                                        .getScheduledEndsAt()
                                                                                                == null
                                                                                        ? ""
                                                                                        : scheduleTime(
                                                                                                match
                                                                                                        .getScheduledEndsAt()),
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

    private static boolean isLegacyGeneratedSoloTeamName(
            final ar.edu.itba.paw.models.TournamentTeam team) {
        if (team.getOrigin() != TournamentTeamOrigin.SOLO_POOL || team.getName() == null) {
            return false;
        }
        final String normalized = team.getName().trim();
        return normalized.matches("(?i)Solo squad #\\d+")
                || normalized.matches("Equipo individual #\\d+");
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

    private static boolean isWinner(
            final ar.edu.itba.paw.models.TournamentTeam team,
            final ar.edu.itba.paw.models.TournamentTeam winner) {
        return team != null && winner != null && Objects.equals(team.getId(), winner.getId());
    }

    private List<TournamentMatchScheduleRequest> allMatchSchedules(
            final TournamentBracketView bracketView, final HttpServletRequest request) {
        return bracketView.getMatches().stream()
                .sorted(
                        Comparator.comparingInt(TournamentMatch::getRoundNumber)
                                .thenComparingInt(TournamentMatch::getMatchIndex))
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

    private static String defaultScheduleStartTime(final int roundNumber) {
        return TIME_INPUT_FORMATTER.format(
                LocalTime.of(18, 0).plusHours(Math.max(0, roundNumber - 1)));
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

    private record TournamentFormConfig(
            String pageTitle,
            String title,
            String description,
            String action,
            String submitLabel,
            String submitLoadingLabel,
            boolean editMode,
            String bannerImageUrl) {}
}
