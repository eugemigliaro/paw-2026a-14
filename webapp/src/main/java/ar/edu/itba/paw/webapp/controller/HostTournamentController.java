package ar.edu.itba.paw.webapp.controller;

import static ar.edu.itba.paw.webapp.utils.ViewFormatUtils.formatInstant;

import ar.edu.itba.paw.models.PlatformTime;
import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.TournamentMatch;
import ar.edu.itba.paw.models.TournamentTeam;
import ar.edu.itba.paw.models.TournamentTeamMember;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.types.TournamentFormat;
import ar.edu.itba.paw.models.types.TournamentPairingStrategy;
import ar.edu.itba.paw.models.types.TournamentStatus;
import ar.edu.itba.paw.services.CreateTournamentRequest;
import ar.edu.itba.paw.services.ImageUpload;
import ar.edu.itba.paw.services.TournamentBracketService;
import ar.edu.itba.paw.services.TournamentBracketView;
import ar.edu.itba.paw.services.TournamentMatchScheduleRequest;
import ar.edu.itba.paw.services.TournamentRegistrationService;
import ar.edu.itba.paw.services.TournamentService;
import ar.edu.itba.paw.services.UpdateTournamentRequest;
import ar.edu.itba.paw.services.exceptions.imageUpload.EmptyImageFileException;
import ar.edu.itba.paw.services.exceptions.imageUpload.ImageTooLargeException;
import ar.edu.itba.paw.services.exceptions.imageUpload.UnsupportedImageFormatException;
import ar.edu.itba.paw.services.exceptions.tournamentBracket.TournamentBracketAlreadyGeneratedException;
import ar.edu.itba.paw.services.exceptions.tournamentBracket.TournamentBracketException;
import ar.edu.itba.paw.services.exceptions.tournamentBracket.TournamentBracketForbiddenException;
import ar.edu.itba.paw.services.exceptions.tournamentBracket.TournamentBracketInvalidPairingsException;
import ar.edu.itba.paw.services.exceptions.tournamentBracket.TournamentBracketInvalidRoundOrderException;
import ar.edu.itba.paw.services.exceptions.tournamentBracket.TournamentBracketInvalidScheduleException;
import ar.edu.itba.paw.services.exceptions.tournamentBracket.TournamentBracketMatchAlreadyDecidedException;
import ar.edu.itba.paw.services.exceptions.tournamentBracket.TournamentBracketMatchNotFoundException;
import ar.edu.itba.paw.services.exceptions.tournamentBracket.TournamentBracketMatchNotReadyException;
import ar.edu.itba.paw.services.exceptions.tournamentBracket.TournamentBracketMissingMatchScheduleException;
import ar.edu.itba.paw.services.exceptions.tournamentBracket.TournamentBracketNotGeneratedException;
import ar.edu.itba.paw.services.exceptions.tournamentBracket.TournamentBracketNotInProgressException;
import ar.edu.itba.paw.services.exceptions.tournamentBracket.TournamentBracketNotReadyForBracketException;
import ar.edu.itba.paw.services.exceptions.tournamentBracket.TournamentBracketPairingStrategyRequiredException;
import ar.edu.itba.paw.services.exceptions.tournamentBracket.TournamentBracketTournamentNotFoundException;
import ar.edu.itba.paw.services.exceptions.tournamentBracket.TournamentBracketUnderCapacityException;
import ar.edu.itba.paw.services.exceptions.tournamentBracket.TournamentBracketWinnerNotInMatchException;
import ar.edu.itba.paw.services.exceptions.tournamentLifecycle.TournamentLifecycleException;
import ar.edu.itba.paw.services.exceptions.tournamentLifecycle.TournamentLifecycleForbiddenException;
import ar.edu.itba.paw.services.exceptions.tournamentLifecycle.TournamentLifecycleInvalidBracketSizeException;
import ar.edu.itba.paw.services.exceptions.tournamentLifecycle.TournamentLifecycleInvalidFormatException;
import ar.edu.itba.paw.services.exceptions.tournamentLifecycle.TournamentLifecycleInvalidJoinModeException;
import ar.edu.itba.paw.services.exceptions.tournamentLifecycle.TournamentLifecycleInvalidRegistrationWindowException;
import ar.edu.itba.paw.services.exceptions.tournamentLifecycle.TournamentLifecycleInvalidScheduleException;
import ar.edu.itba.paw.services.exceptions.tournamentLifecycle.TournamentLifecycleInvalidTeamSizeException;
import ar.edu.itba.paw.services.exceptions.tournamentLifecycle.TournamentLifecycleNotCancellableException;
import ar.edu.itba.paw.services.exceptions.tournamentLifecycle.TournamentLifecycleNotEditableException;
import ar.edu.itba.paw.services.exceptions.tournamentRegistration.TournamentRegistrationAlreadyAssignedException;
import ar.edu.itba.paw.services.exceptions.tournamentRegistration.TournamentRegistrationAlreadyOnTeamException;
import ar.edu.itba.paw.services.exceptions.tournamentRegistration.TournamentRegistrationException;
import ar.edu.itba.paw.services.exceptions.tournamentRegistration.TournamentRegistrationForbiddenException;
import ar.edu.itba.paw.services.exceptions.tournamentRegistration.TournamentRegistrationNotInSoloPoolException;
import ar.edu.itba.paw.services.exceptions.tournamentRegistration.TournamentRegistrationNotOpenException;
import ar.edu.itba.paw.services.exceptions.tournamentRegistration.TournamentRegistrationSoloPoolFullException;
import ar.edu.itba.paw.services.exceptions.tournamentRegistration.TournamentRegistrationSoloSignupDisabledException;
import ar.edu.itba.paw.services.exceptions.tournamentRegistration.TournamentRegistrationTournamentNotFoundException;
import ar.edu.itba.paw.services.exceptions.tournamentRegistration.TournamentRegistrationUnderCapacityException;
import ar.edu.itba.paw.webapp.form.BracketManualPairingsForm;
import ar.edu.itba.paw.webapp.form.BracketPublishForm;
import ar.edu.itba.paw.webapp.form.BracketPublishScheduleForm;
import ar.edu.itba.paw.webapp.form.CreateTournamentForm;
import ar.edu.itba.paw.webapp.security.annotation.AuthenticatedUser;
import ar.edu.itba.paw.webapp.utils.ImageUrlHelper;
import ar.edu.itba.paw.webapp.viewmodel.TournamentBracketViewModel;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class HostTournamentController {

    private static final double DEFAULT_MAP_LATITUDE = -34.6037;
    private static final double DEFAULT_MAP_LONGITUDE = -58.3816;
    private static final int DEFAULT_MAP_ZOOM = 14;

    private static final DateTimeFormatter TIME_INPUT_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm");

    private final TournamentService tournamentService;
    private final TournamentRegistrationService tournamentRegistrationService;
    private final TournamentBracketService tournamentBracketService;
    private final MessageSource messageSource;
    private final boolean mapPickerEnabled;
    private final String mapTileUrlTemplate;
    private final String mapAttribution;
    private final double mapDefaultLatitude;
    private final double mapDefaultLongitude;
    private final int mapDefaultZoom;

    @Autowired
    public HostTournamentController(
            final TournamentService tournamentService,
            final TournamentRegistrationService tournamentRegistrationService,
            final TournamentBracketService tournamentBracketService,
            final MessageSource messageSource,
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
        this.messageSource = messageSource;
        this.mapPickerEnabled = mapPickerEnabled;
        this.mapTileUrlTemplate = mapTileUrlTemplate == null ? "" : mapTileUrlTemplate;
        this.mapAttribution = mapAttribution == null ? "" : mapAttribution;
        this.mapDefaultLatitude = mapDefaultLatitude;
        this.mapDefaultLongitude = mapDefaultLongitude;
        this.mapDefaultZoom = mapDefaultZoom;
    }

    @ModelAttribute("createTournamentForm")
    public CreateTournamentForm createTournamentForm() {
        return new CreateTournamentForm();
    }

    @GetMapping("/host/tournaments/new")
    public ModelAndView showCreateTournament(final Locale locale) {
        return createFormView(createTournamentForm(), null, locale);
    }

    @PostMapping("/host/tournaments")
    public ModelAndView createTournament(
            @AuthenticatedUser final User user,
            @Valid @ModelAttribute("createTournamentForm")
                    final CreateTournamentForm createTournamentForm,
            final BindingResult bindingResult,
            final Locale locale) {

        if (bindingResult.hasErrors()) {
            return createFormView(createTournamentForm, null, locale, createFormConfig(locale));
        }

        final CreateTournamentRequest request =
                new CreateTournamentRequest(
                        createTournamentForm.getSport(),
                        createTournamentForm.getTitle(),
                        createTournamentForm.getDescription(),
                        createTournamentForm.getAddress(),
                        createTournamentForm.getLatitude(),
                        createTournamentForm.getLongitude(),
                        createTournamentForm.getStartDate(),
                        createTournamentForm.getStartTime(),
                        createTournamentForm.getEndDate(),
                        createTournamentForm.getEndTime(),
                        createTournamentForm.getPricePerPlayer(),
                        bannerUpload(createTournamentForm.getBannerImage()),
                        TournamentFormat.SINGLE_ELIMINATION,
                        createTournamentForm.getBracketSize(),
                        createTournamentForm.getTeamSize(),
                        createTournamentForm.isAllowSoloSignup(),
                        createTournamentForm.isAllowTeamDraft(),
                        createTournamentForm.getRegistrationOpensDate(),
                        createTournamentForm.getRegistrationOpensTime(),
                        createTournamentForm.getRegistrationClosesDate(),
                        createTournamentForm.getRegistrationClosesTime());

        try {
            final Tournament createdTournament = tournamentService.createTournament(user, request);
            return seeOther("/tournaments/" + createdTournament.getId());
        } catch (final UnsupportedImageFormatException exception) {
            rejectBannerImage(bindingResult, "host.form.bannerImage.error.invalidFormat");
            return createFormView(createTournamentForm, null, locale, createFormConfig(locale));
        } catch (final EmptyImageFileException exception) {
            rejectBannerImage(bindingResult, "host.form.bannerImage.error.empty");
            return createFormView(createTournamentForm, null, locale, createFormConfig(locale));
        } catch (final ImageTooLargeException exception) {
            rejectBannerImage(bindingResult, "host.form.bannerImage.error.tooLarge");
            return createFormView(createTournamentForm, null, locale, createFormConfig(locale));
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
            @AuthenticatedUser final User user,
            @PathVariable("tournamentId") final Long tournamentId,
            final Locale locale) {
        final Tournament tournament =
                tournamentService
                        .findTournamentForHost(tournamentId, user)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!isEditable(tournament)) { // TODO: is not found the correct response here?
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return createFormView(toForm(tournament), null, locale, editFormConfig(tournament, locale));
    }

    @PostMapping("/host/tournaments/{tournamentId:\\d+}/edit")
    public ModelAndView updateTournament(
            @AuthenticatedUser final User user,
            @PathVariable("tournamentId") final Long tournamentId,
            @Valid @ModelAttribute("createTournamentForm")
                    final CreateTournamentForm createTournamentForm,
            final BindingResult bindingResult,
            final Locale locale,
            final RedirectAttributes redirectAttributes) {
        final Tournament tournament =
                tournamentService
                        .findTournamentForHost(tournamentId, user)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        final TournamentFormConfig formConfig = editFormConfig(tournament, locale);

        if (bindingResult.hasErrors()) {
            return createFormView(createTournamentForm, null, locale, formConfig);
        }

        final UpdateTournamentRequest request =
                new UpdateTournamentRequest(
                        createTournamentForm.getSport(),
                        createTournamentForm.getTitle(),
                        createTournamentForm.getDescription(),
                        createTournamentForm.getAddress(),
                        createTournamentForm.getLatitude(),
                        createTournamentForm.getLongitude(),
                        createTournamentForm.getStartDate(),
                        createTournamentForm.getStartTime(),
                        createTournamentForm.getEndDate(),
                        createTournamentForm.getEndTime(),
                        createTournamentForm.getPricePerPlayer(),
                        bannerUpload(createTournamentForm.getBannerImage()),
                        createTournamentForm.getBracketSize(),
                        createTournamentForm.getTeamSize(),
                        createTournamentForm.getRegistrationOpensDate(),
                        createTournamentForm.getRegistrationOpensTime(),
                        createTournamentForm.getRegistrationClosesDate(),
                        createTournamentForm.getRegistrationClosesTime());

        try {
            tournamentService.update(
                    tournamentId,
                    user,
                    request); // TODO: check if it's editable before calling service
        } catch (final TournamentLifecycleForbiddenException exception) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        } catch (final UnsupportedImageFormatException exception) {
            rejectBannerImage(bindingResult, "host.form.bannerImage.error.invalidFormat");
            return createFormView(createTournamentForm, null, locale, formConfig);
        } catch (final EmptyImageFileException exception) {
            rejectBannerImage(bindingResult, "host.form.bannerImage.error.empty");
            return createFormView(createTournamentForm, null, locale, formConfig);
        } catch (final ImageTooLargeException exception) {
            rejectBannerImage(bindingResult, "host.form.bannerImage.error.tooLarge");
            return createFormView(createTournamentForm, null, locale, formConfig);
        } catch (final TournamentLifecycleException exception) {
            applyServiceError(exception, bindingResult, locale);
            return createFormView(createTournamentForm, null, locale, formConfig);
        }

        redirectAttributes.addFlashAttribute(
                "tournamentNoticeCode", "tournament.host.edit.success");
        return seeOther("/tournaments/" + tournamentId);
    }

    @PostMapping("/host/tournaments/{tournamentId:\\d+}/close-registration")
    public ModelAndView closeRegistration(
            @AuthenticatedUser final User user,
            @PathVariable("tournamentId") final Long tournamentId,
            final RedirectAttributes redirectAttributes) {

        try {
            final Tournament tournament =
                    tournamentRegistrationService.closeRegistration(tournamentId, user);
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
        } catch (final TournamentRegistrationTournamentNotFoundException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        } catch (final TournamentRegistrationForbiddenException exception) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        } catch (final TournamentRegistrationException exception) {
            redirectAttributes.addFlashAttribute(
                    "tournamentErrorCode", registrationErrorCode(exception));
            return new ModelAndView("redirect:/tournaments/" + tournamentId);
        }
    }

    @PostMapping("/host/tournaments/{tournamentId:\\d+}/bracket/strategy")
    public ModelAndView updateBracketStrategy(
            @AuthenticatedUser final User user,
            @PathVariable("tournamentId") final Long tournamentId,
            @RequestParam("pairingStrategy") final TournamentPairingStrategy pairingStrategy,
            final RedirectAttributes redirectAttributes) {
        try {
            tournamentBracketService.updatePairingStrategy(tournamentId, user, pairingStrategy);
            redirectAttributes.addFlashAttribute(
                    "tournamentNoticeCode", "tournament.bracket.strategy.updated");
        } catch (final TournamentBracketException exception) {
            handleBracketException(exception, redirectAttributes);
        }
        return new ModelAndView("redirect:/host/tournaments/" + tournamentId + "/bracket/setup");
    }

    @PostMapping("/host/tournaments/{tournamentId:\\d+}/cancel")
    public ModelAndView cancelTournament(
            @AuthenticatedUser final User user,
            @PathVariable("tournamentId") final Long tournamentId,
            final RedirectAttributes redirectAttributes) {
        try {
            tournamentService.cancel(
                    tournamentId, user, "Host cancelled tournament"); // TODO: reason not localized
            redirectAttributes.addFlashAttribute(
                    "tournamentNoticeCode", "tournament.host.cancel.success");
            return new ModelAndView("redirect:/tournaments/" + tournamentId);
        } catch (final TournamentLifecycleForbiddenException exception) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        } catch (final TournamentLifecycleException exception) {
            redirectAttributes.addFlashAttribute(
                    "tournamentErrorCode", lifecycleErrorCode(exception));
            return new ModelAndView("redirect:/tournaments/" + tournamentId);
        }
    }

    @PostMapping("/host/tournaments/{tournamentId:\\d+}/bracket/generate")
    public ModelAndView generateBracket(
            @AuthenticatedUser final User user,
            @PathVariable("tournamentId") final Long tournamentId,
            final RedirectAttributes redirectAttributes) {
        try {
            tournamentBracketService.generateBracket(tournamentId, user);
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
            @AuthenticatedUser final User user,
            @PathVariable("tournamentId") final Long tournamentId,
            final Model model,
            final Locale locale) {
        final Tournament tournament =
                tournamentService
                        .findTournamentForHost(tournamentId, user)
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
                            tournamentBracketService.getBracket(tournamentId, user), locale);
        } catch (final TournamentBracketNotGeneratedException exception) {
            bracketPage = buildUngeneratedBracketPage(tournament, user, locale);
        } catch (final TournamentBracketException exception) {
            handleBracketException(exception, null);
            throw exception;
        }

        final List<TournamentTeam> manualPairingTeams =
                bracketPage.isGenerated()
                        ? List.of()
                        : tournamentBracketService.listTeamsForSetup(tournamentId, user);
        final BracketPublishForm publishForm =
                bracketPage.isGenerated() ? createBracketPublishForm(bracketPage) : null;
        final BracketManualPairingsForm manualPairingsForm =
                bracketPage.isGenerated() ? null : createManualPairingsForm(manualPairingTeams);

        return bracketSetupView(
                tournamentId,
                tournament,
                bracketPage,
                manualPairingTeams,
                publishForm,
                manualPairingsForm,
                locale,
                flashString(model, "tournamentNoticeCode").orElse(null),
                flashString(model, "tournamentErrorCode").orElse(null));
    }

    @PostMapping("/host/tournaments/{tournamentId:\\d+}/bracket/manual-pairings")
    public ModelAndView saveManualPairings(
            @AuthenticatedUser final User user,
            @PathVariable("tournamentId") final Long tournamentId,
            @Valid @ModelAttribute("manualPairingsForm")
                    final BracketManualPairingsForm manualPairingsForm,
            final BindingResult bindingResult,
            final Locale locale,
            final RedirectAttributes redirectAttributes) {
        final Tournament tournament =
                tournamentService
                        .findTournamentForHost(tournamentId, user)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        try {
            if (bindingResult.hasErrors()) {
                final TournamentBracketViewModel bracketPage =
                        buildUngeneratedBracketPage(tournament, user, locale);
                final List<TournamentTeam> manualPairingTeams =
                        tournamentBracketService.listTeamsForSetup(tournamentId, user);
                return bracketSetupView(
                        tournamentId,
                        tournament,
                        bracketPage,
                        manualPairingTeams,
                        null,
                        manualPairingsForm,
                        locale,
                        null,
                        null);
            }

            tournamentBracketService.saveManualPairings(
                    tournamentId, user, manualPairingsForm.getTeamIds());
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
            @AuthenticatedUser final User user,
            @PathVariable("tournamentId") final Long tournamentId,
            @Valid @ModelAttribute("bracketPublishForm")
                    final BracketPublishForm bracketPublishForm,
            final BindingResult bindingResult,
            final Locale locale,
            final RedirectAttributes redirectAttributes) {
        final Tournament tournament =
                tournamentService
                        .findTournamentForHost(tournamentId, user)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        try {
            final TournamentBracketView bracketView =
                    tournamentBracketService.getBracket(tournamentId, user);
            if (bindingResult.hasErrors()) {
                final TournamentBracketViewModel bracketPage =
                        buildBracketPage(bracketView, locale);
                return bracketSetupView(
                        tournamentId,
                        tournament,
                        bracketPage,
                        List.of(),
                        bracketPublishForm,
                        null,
                        locale,
                        null,
                        null);
            }

            final List<TournamentMatchScheduleRequest> schedules =
                    toMatchScheduleRequests(bracketPublishForm);
            tournamentBracketService.publishBracket(tournamentId, user, schedules);
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

    private ModelAndView bracketSetupView(
            final long tournamentId,
            final Tournament tournament,
            final TournamentBracketViewModel bracketPage,
            final List<TournamentTeam> manualPairingTeams,
            final BracketPublishForm bracketPublishForm,
            final BracketManualPairingsForm manualPairingsForm,
            final Locale locale,
            final String tournamentNoticeCode,
            final String tournamentErrorCode) {
        final ModelAndView mav = new ModelAndView("host/tournaments/bracket-setup");
        mav.addObject(
                "pageTitle",
                messageSource.getMessage(
                        "page.title.hostTournamentBracketSetup",
                        new Object[] {tournament.getTitle()},
                        locale));
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
        mav.addObject(
                "manualPairingTeams", manualPairingTeams == null ? List.of() : manualPairingTeams);
        mav.addObject(
                "publishBracketPath", "/host/tournaments/" + tournamentId + "/bracket/publish");
        mav.addObject("tournamentDetailPath", "/tournaments/" + tournamentId);
        mav.addObject("bracketPublishForm", bracketPublishForm);
        mav.addObject("manualPairingsForm", manualPairingsForm);
        mav.addObject("tournamentNoticeCode", tournamentNoticeCode);
        mav.addObject("tournamentErrorCode", tournamentErrorCode);
        return mav;
    }

    private BracketPublishForm createBracketPublishForm(
            final TournamentBracketViewModel bracketPage) {
        final BracketPublishForm form = new BracketPublishForm();
        final List<BracketPublishScheduleForm> schedules = new ArrayList<>();
        for (final TournamentBracketViewModel.RoundViewModel round : bracketPage.getRounds()) {
            for (final TournamentBracketViewModel.MatchViewModel match : round.getMatches()) {
                final BracketPublishScheduleForm schedule = new BracketPublishScheduleForm();
                schedule.setMatchId(match.getId());
                schedule.setRoundNumber(round.getRoundNumber());
                schedule.setRoundLabel(round.getLabel());
                schedule.setMatchLabel(match.getLabel());
                schedule.setStartDate(parseDate(match.getStartDate()));
                schedule.setStartTime(parseTime(match.getStartTime()));
                schedule.setEndDate(parseDate(match.getEndDate()));
                schedule.setEndTime(parseTime(match.getEndTime()));
                schedule.setAddress(match.getAddress());
                schedule.setLatitude(parseDouble(match.getLatitude()));
                schedule.setLongitude(parseDouble(match.getLongitude()));
                schedules.add(schedule);
            }
        }
        form.setSchedules(schedules);
        return form;
    }

    private BracketManualPairingsForm createManualPairingsForm(
            final List<TournamentTeam> manualPairingTeams) {
        final BracketManualPairingsForm form = new BracketManualPairingsForm();
        final List<Long> teamIds = new ArrayList<>();
        for (final TournamentTeam team : manualPairingTeams) {
            if (team != null) {
                teamIds.add(team.getId());
            }
        }
        form.setTeamIds(teamIds);
        form.setExpectedTeamCount(teamIds.size());
        return form;
    }

    private List<TournamentMatchScheduleRequest> toMatchScheduleRequests(
            final BracketPublishForm form) {
        final List<TournamentMatchScheduleRequest> schedules = new ArrayList<>();
        for (final BracketPublishScheduleForm schedule : form.getSchedules()) {
            schedules.add(
                    new TournamentMatchScheduleRequest(
                            schedule.getMatchId(),
                            schedule.getStartDate(),
                            schedule.getStartTime(),
                            schedule.getEndDate(),
                            schedule.getEndTime(),
                            schedule.getAddress(),
                            schedule.getLatitude(),
                            schedule.getLongitude()));
        }
        return schedules;
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

    private CreateTournamentForm toForm(final Tournament tournament) {
        final CreateTournamentForm form = new CreateTournamentForm();
        form.setStartDate(null);
        form.setStartTime(null);
        form.setEndDate(null);
        form.setEndTime(null);
        form.setTitle(tournament.getTitle());
        form.setDescription(tournament.getDescription());
        form.setAddress(tournament.getAddress());
        form.setLatitude(tournament.getLatitude());
        form.setLongitude(tournament.getLongitude());
        form.setSport(tournament.getSport());
        form.setBracketSize(tournament.getBracketSize());
        form.setTeamSize(tournament.getTeamSize());
        form.setAllowSoloSignup(tournament.isAllowSoloSignup());
        form.setAllowTeamDraft(tournament.isAllowTeamDraft());
        form.setPricePerPlayer(tournament.getPricePerPlayer());
        form.setRegistrationOpensDate(tournament.getRegistrationOpensAtDateTime().toLocalDate());
        form.setRegistrationOpensTime(tournament.getRegistrationOpensAtDateTime().toLocalTime());
        form.setRegistrationClosesDate(tournament.getRegistrationClosesAtDateTime().toLocalDate());
        form.setRegistrationClosesTime(tournament.getRegistrationClosesAtDateTime().toLocalTime());
        final OffsetDateTime startsAt = tournament.getStartsAtDateTime();
        if (startsAt != null) {
            form.setStartDate(startsAt.toLocalDate());
            form.setStartTime(startsAt.toLocalTime());
        }
        final OffsetDateTime endsAt = tournament.getEndsAtDateTime();
        if (endsAt != null) {
            form.setEndDate(endsAt.toLocalDate());
            form.setEndTime(endsAt.toLocalTime());
        }
        return form;
    }

    private ImageUpload bannerUpload(
            final MultipartFile
                    bannerImage) { // TODO: move this to a different file. It also used in other
        // controllers
        if (bannerImage == null) {
            return null;
        }
        return new ImageUpload() {
            @Override
            public String getContentType() {
                return bannerImage.getContentType();
            }

            @Override
            public long getContentLength() {
                return bannerImage.getSize();
            }

            @Override
            public String getOriginalFilename() {
                return bannerImage.getOriginalFilename();
            }

            @Override
            public java.io.InputStream getContentStream() throws java.io.IOException {
                return bannerImage.getInputStream();
            }
        };
    }

    private static ModelAndView seeOther(final String targetUrl) {
        final RedirectView redirectView = new RedirectView(targetUrl, true);
        redirectView.setStatusCode(HttpStatus.SEE_OTHER);
        return new ModelAndView(redirectView);
    }

    private boolean isEditable(final Tournament tournament) {
        return TournamentStatus.REGISTRATION == tournament.getStatus();
    }

    private void applyServiceError(
            final TournamentLifecycleException exception,
            final BindingResult bindingResult,
            final Locale locale) {
        final String code = lifecycleErrorCode(exception);
        if (exception instanceof TournamentLifecycleInvalidBracketSizeException) {
            bindingResult.rejectValue("bracketSize", code);
        } else if (exception instanceof TournamentLifecycleInvalidTeamSizeException) {
            bindingResult.rejectValue("teamSize", code);
        } else if (exception instanceof TournamentLifecycleInvalidRegistrationWindowException) {
            bindingResult.rejectValue("registrationClosesTime", code);
        } else if (exception instanceof TournamentLifecycleInvalidJoinModeException) {
            bindingResult.rejectValue("allowSoloSignup", code);
        } else {
            bindingResult.reject(
                    "CreateTournamentForm.global",
                    messageSource.getMessage(code, null, code, locale));
        }
    }

    private static void rejectBannerImage(final BindingResult bindingResult, final String code) {
        bindingResult.rejectValue("bannerImage", code);
    }

    private static String lifecycleErrorCode(final TournamentLifecycleException exception) {
        return switch (exception) {
            case TournamentLifecycleInvalidBracketSizeException ignored ->
                    "tournament.lifecycle.error.invalidBracketSize";
            case TournamentLifecycleInvalidTeamSizeException ignored ->
                    "tournament.lifecycle.error.invalidTeamSize";
            case TournamentLifecycleInvalidJoinModeException ignored ->
                    "tournament.lifecycle.error.invalidJoinMode";
            case TournamentLifecycleInvalidRegistrationWindowException ignored ->
                    "tournament.lifecycle.error.invalidRegistrationWindow";
            case TournamentLifecycleInvalidScheduleException ignored ->
                    "tournament.lifecycle.error.invalidSchedule";
            case TournamentLifecycleInvalidFormatException ignored ->
                    "tournament.lifecycle.error.invalidFormat";
            case TournamentLifecycleForbiddenException ignored ->
                    "tournament.lifecycle.error.forbidden";
            case TournamentLifecycleNotEditableException ignored ->
                    "tournament.lifecycle.error.notEditable";
            case TournamentLifecycleNotCancellableException ignored ->
                    "tournament.lifecycle.error.notCancellable";
            default -> "tournament.lifecycle.error.invalidDetails";
        };
    }

    private static String registrationErrorCode(final TournamentRegistrationException exception) {
        return switch (exception) {
            case TournamentRegistrationSoloSignupDisabledException ignored ->
                    "tournament.registration.error.soloDisabled";
            case TournamentRegistrationNotOpenException ignored ->
                    "tournament.registration.error.notOpen";
            case TournamentRegistrationAlreadyOnTeamException ignored ->
                    "tournament.registration.error.alreadyOnTeam";
            case TournamentRegistrationAlreadyAssignedException ignored ->
                    "tournament.registration.error.alreadyAssigned";
            case TournamentRegistrationNotInSoloPoolException ignored ->
                    "tournament.registration.error.notInSoloPool";
            case TournamentRegistrationSoloPoolFullException ignored ->
                    "tournament.registration.error.soloPoolFull";
            case TournamentRegistrationUnderCapacityException ignored ->
                    "tournament.registration.error.underCapacity";
            default -> "tournament.registration.error.notFound";
        };
    }

    private void handleBracketException(
            final TournamentBracketException exception,
            final RedirectAttributes redirectAttributes) {
        if (exception instanceof TournamentBracketTournamentNotFoundException) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (exception instanceof TournamentBracketForbiddenException) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (redirectAttributes != null) {
            redirectAttributes.addFlashAttribute(
                    "tournamentErrorCode", bracketErrorCode(exception));
        }
    }

    private static String bracketErrorCode(final TournamentBracketException exception) {
        return switch (exception) {
            case TournamentBracketMatchNotFoundException ignored ->
                    "tournament.bracket.error.matchNotFound";
            case TournamentBracketNotReadyForBracketException ignored ->
                    "tournament.bracket.error.notReady";
            case TournamentBracketAlreadyGeneratedException ignored ->
                    "tournament.bracket.error.alreadyGenerated";
            case TournamentBracketNotGeneratedException ignored ->
                    "tournament.bracket.error.notGenerated";
            case TournamentBracketPairingStrategyRequiredException ignored ->
                    "tournament.bracket.error.pairingStrategyRequired";
            case TournamentBracketInvalidPairingsException ignored ->
                    "tournament.bracket.error.invalidPairings";
            case TournamentBracketUnderCapacityException ignored ->
                    "tournament.bracket.error.underCapacity";
            case TournamentBracketMissingMatchScheduleException ignored ->
                    "tournament.bracket.error.missingMatchSchedule";
            case TournamentBracketInvalidScheduleException ignored ->
                    "tournament.bracket.error.invalidSchedule";
            case TournamentBracketInvalidRoundOrderException ignored ->
                    "tournament.bracket.error.invalidRoundOrder";
            case TournamentBracketNotInProgressException ignored ->
                    "tournament.bracket.error.notInProgress";
            case TournamentBracketMatchNotReadyException ignored ->
                    "tournament.bracket.error.matchNotReady";
            case TournamentBracketMatchAlreadyDecidedException ignored ->
                    "tournament.bracket.error.matchAlreadyDecided";
            case TournamentBracketWinnerNotInMatchException ignored ->
                    "tournament.bracket.error.winnerNotInMatch";
            case TournamentBracketForbiddenException ignored ->
                    "tournament.bracket.error.forbidden";
            default -> "tournament.bracket.error.notFound";
        };
    }

    private TournamentBracketViewModel buildUngeneratedBracketPage(
            final Tournament tournament, final User actingUser, final Locale locale) {
        final List<TournamentTeam> teams =
                tournamentBracketService.listTeamsForSetup(tournament.getId(), actingUser);
        final List<TournamentTeamMember> teamMembers =
                tournamentRegistrationService.listTeamMembers(tournament.getId());
        final Map<Long, Integer> teamDisplayNumbers = teamDisplayNumbers(teams);
        return new TournamentBracketViewModel(
                tournament.getId(),
                tournament.getTitle(),
                statusLabel(tournament, locale),
                statusTone(tournament),
                false,
                false,
                false,
                List.of(),
                teamRosters(
                        new TournamentBracketView(
                                tournament, teams, List.of(), null, null, teamMembers),
                        locale,
                        teamDisplayNumbers));
    }

    private TournamentBracketViewModel buildBracketPage(
            final TournamentBracketView bracketView, final Locale locale) {
        final Tournament tournament = bracketView.getTournament();
        final String defaultScheduleDate = LocalDate.now(PlatformTime.ZONE).plusDays(1).toString();
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
                                                                                        locale,
                                                                                        teamDisplayNumbers),
                                                                                teamName(
                                                                                        match
                                                                                                .getTeamB(),
                                                                                        locale,
                                                                                        teamDisplayNumbers),
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
                rounds,
                teamRosters(bracketView, locale, teamDisplayNumbers));
    }

    private List<TournamentBracketViewModel.TeamRosterViewModel> teamRosters(
            final TournamentBracketView bracketView,
            final Locale locale,
            final Map<Long, Integer> teamDisplayNumbers) {
        final Map<Long, List<String>> usernamesByTeamId = new LinkedHashMap<>();
        for (final TournamentTeamMember member : bracketView.getTeamMembers()) {
            if (member.getTeam() == null || member.getUser() == null) {
                continue;
            }
            usernamesByTeamId
                    .computeIfAbsent(member.getTeam().getId(), ignored -> new ArrayList<>())
                    .add(member.getUser().getUsername());
        }

        return bracketView.getTeams().stream()
                .map(
                        team ->
                                new TournamentBracketViewModel.TeamRosterViewModel(
                                        teamName(team, locale, teamDisplayNumbers),
                                        usernamesByTeamId.getOrDefault(team.getId(), List.of())))
                .toList();
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

    private static Long teamId(final TournamentTeam team) {
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
            return formatInstant(match.getScheduledStartsAt(), locale, PlatformTime.ZONE);
        }
        return messageSource.getMessage(
                "tournament.bracket.schedule.range",
                new Object[] {
                    formatInstant(match.getScheduledStartsAt(), locale, PlatformTime.ZONE),
                    formatInstant(match.getScheduledEndsAt(), locale, PlatformTime.ZONE)
                },
                locale);
    }

    private static boolean isWinner(final TournamentTeam team, final TournamentTeam winner) {
        return team != null && winner != null && Objects.equals(team.getId(), winner.getId());
    }

    private static String defaultScheduleStartTime(final int roundNumber) {
        return TIME_INPUT_FORMATTER.format(
                LocalTime.of(18, 0).plusHours(Math.max(0, roundNumber - 1)));
    }

    private static String scheduleDate(final Instant instant) {
        return instant == null
                ? ""
                : PlatformTime.toOffsetDateTime(instant).toLocalDate().toString();
    }

    private static String scheduleTime(final Instant instant) {
        return instant == null
                ? ""
                : TIME_INPUT_FORMATTER.format(PlatformTime.toOffsetDateTime(instant).toLocalTime());
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

    private static LocalDate parseDate(
            final String value) { // TODO: remove when replacing viewModels
        return value == null || value.isBlank() ? null : LocalDate.parse(value);
    }

    private static LocalTime parseTime(
            final String value) { // TODO: remove when replacing viewModels
        return value == null || value.isBlank() ? null : LocalTime.parse(value);
    }

    private static Double parseDouble(
            final String value) { // TODO: remove when replacing viewModels
        final String normalized = normalizeBlank(value);
        if (normalized.isEmpty()) {
            return null;
        }
        return Double.valueOf(normalized);
    }

    private static Optional<String> flashString(final Model model, final String name) {
        final Object value = model.asMap().get(name);
        return value instanceof String ? Optional.of((String) value) : Optional.empty();
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
