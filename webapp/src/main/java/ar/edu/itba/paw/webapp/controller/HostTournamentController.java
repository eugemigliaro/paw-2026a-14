package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.PlatformTime;
import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.TournamentMatch;
import ar.edu.itba.paw.models.TournamentTeam;
import ar.edu.itba.paw.models.TournamentTeamMember;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.exceptions.tournamentBracket.TournamentBracketException;
import ar.edu.itba.paw.models.exceptions.tournamentBracket.TournamentBracketNotGeneratedException;
import ar.edu.itba.paw.models.exceptions.tournamentLifecycle.TournamentLifecycleException;
import ar.edu.itba.paw.models.exceptions.tournamentRegistration.TournamentRegistrationException;
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
import ar.edu.itba.paw.webapp.form.BracketManualPairingsForm;
import ar.edu.itba.paw.webapp.form.BracketPublishForm;
import ar.edu.itba.paw.webapp.form.BracketPublishScheduleForm;
import ar.edu.itba.paw.webapp.form.CreateTournamentForm;
import ar.edu.itba.paw.webapp.security.annotation.AuthenticatedUser;
import ar.edu.itba.paw.webapp.utils.ImageUrlHelper;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

        final Tournament createdTournament = tournamentService.createTournament(user, request);
        return seeOther("/tournaments/" + createdTournament.getId());
    }

    @GetMapping("/host/tournaments/{tournamentId:\\d+}/edit")
    public ModelAndView showEditTournament(
            @AuthenticatedUser final User user,
            @PathVariable("tournamentId") final Long tournamentId,
            final Locale locale) {
        final Tournament tournament =
                tournamentService
                        .findEditableTournamentForHost(tournamentId, user)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
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
                        .findEditableTournamentForHost(tournamentId, user)
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

        tournamentService.update(tournamentId, user, request);
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
        } catch (final TournamentRegistrationException e) {
            final String errorMsg = "tournament.registration.error." + e.getMessage();
            redirectAttributes.addFlashAttribute("tournamentErrorCode", errorMsg);
            return new ModelAndView("redirect:/tournaments/" + tournamentId);
        }
    }

    @PostMapping("/host/tournaments/{tournamentId:\\d+}/bracket/strategy")
    public ModelAndView updateBracketStrategy(
            @AuthenticatedUser final User user,
            @PathVariable("tournamentId") final Long tournamentId,
            @RequestParam(value = "pairingStrategy", required = true)
                    final TournamentPairingStrategy pairingStrategy,
            final RedirectAttributes redirectAttributes) {
        try {
            tournamentBracketService.updatePairingStrategy(tournamentId, user, pairingStrategy);
            redirectAttributes.addFlashAttribute(
                    "tournamentNoticeCode", "tournament.bracket.strategy.updated");
        } catch (final TournamentBracketException e) {
            final String errorMsg = "tournament.bracket.error." + e.getMessage();
            redirectAttributes.addFlashAttribute("tournamentErrorCode", errorMsg);
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
        } catch (final TournamentLifecycleException e) {
            final String errorMsg = "tournament.lifecycle.error." + e.getMessage();
            redirectAttributes.addFlashAttribute("tournamentErrorCode", errorMsg);
        }
        return new ModelAndView("redirect:/tournaments/" + tournamentId);
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
        } catch (final TournamentBracketException e) {
            final String errorMsg = "tournament.bracket.error." + e.getMessage();
            redirectAttributes.addFlashAttribute("tournamentErrorCode", errorMsg);
        }
        return new ModelAndView("redirect:/host/tournaments/" + tournamentId + "/bracket/setup");
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

        TournamentBracketView bracketView = null;
        boolean bracketGenerated = true;
        try {
            bracketView = tournamentBracketService.getBracket(tournamentId, user);
        } catch (final TournamentBracketNotGeneratedException exception) {
            bracketGenerated = false;
        }

        final List<TournamentTeam> manualPairingTeams =
                bracketGenerated
                        ? List.of()
                        : tournamentBracketService.listTeamsForSetup(tournamentId, user);
        final BracketPublishForm publishForm =
                bracketGenerated ? createBracketPublishForm(bracketView) : null;
        final BracketManualPairingsForm manualPairingsForm =
                bracketGenerated ? null : createManualPairingsForm(manualPairingTeams);
        final List<TournamentTeamMember> teamMembers =
                bracketGenerated
                        ? bracketView.getTeamMembers()
                        : tournamentRegistrationService.listTeamMembers(tournamentId);

        return bracketSetupView(
                tournamentId,
                tournament,
                bracketView,
                bracketGenerated,
                teamMembers,
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
                final List<TournamentTeam> manualPairingTeams =
                        tournamentBracketService.listTeamsForSetup(tournamentId, user);
                final List<TournamentTeamMember> teamMembers =
                        tournamentRegistrationService.listTeamMembers(tournamentId);
                return bracketSetupView(
                        tournamentId,
                        tournament,
                        null,
                        false,
                        teamMembers,
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
        } catch (final TournamentBracketException e) {
            final String errorMsg = "tournament.bracket.error." + e.getMessage();
            redirectAttributes.addFlashAttribute("tournamentErrorCode", errorMsg);
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
                return bracketSetupView(
                        tournamentId,
                        tournament,
                        bracketView,
                        true,
                        bracketView.getTeamMembers(),
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
        } catch (final TournamentBracketException e) {
            final String errorMsg = "tournament.bracket.error." + e.getMessage();
            redirectAttributes.addFlashAttribute("tournamentErrorCode", errorMsg);
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
        mav.addObject("pageTitleCode", formConfig.pageTitleCode());
        mav.addObject("pageTitleArgument", formConfig.pageTitleArgument());
        mav.addObject("createTournamentForm", form);
        mav.addObject("formError", formError);
        mav.addObject("formTitleCode", formConfig.titleCode());
        mav.addObject("formDescriptionCode", formConfig.descriptionCode());
        mav.addObject("formAction", formConfig.action());
        mav.addObject("submitLabelCode", formConfig.submitLabelCode());
        mav.addObject("submitLoadingLabelCode", formConfig.submitLoadingLabelCode());
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
            final TournamentBracketView bracketView,
            final boolean bracketGenerated,
            final List<TournamentTeamMember> teamMembers,
            final List<TournamentTeam> manualPairingTeams,
            final BracketPublishForm bracketPublishForm,
            final BracketManualPairingsForm manualPairingsForm,
            final Locale locale,
            final String tournamentNoticeCode,
            final String tournamentErrorCode) {
        final ModelAndView mav = new ModelAndView("host/tournaments/bracket-setup");
        final List<TournamentTeam> bracketTeams =
                bracketGenerated && bracketView != null
                        ? bracketView.getTeams()
                        : manualPairingTeams == null ? List.of() : manualPairingTeams;
        final Map<Long, Integer> teamDisplayNumbers = teamDisplayNumbers(bracketTeams);
        final Map<Integer, List<TournamentMatch>> matchesByRound =
                bracketView == null ? Map.of() : matchesByRound(bracketView.getMatches());
        mav.addObject("bracketTournament", tournament);
        mav.addObject("bracketView", bracketView);
        mav.addObject("bracketGenerated", bracketGenerated);
        mav.addObject(
                "bracketPublishable", TournamentStatus.BRACKET_SETUP == tournament.getStatus());
        mav.addObject("bracketTeams", bracketTeams);
        mav.addObject("bracketMembersByTeamId", membersByTeamId(teamMembers));
        mav.addObject("bracketTeamDisplayNumbers", teamDisplayNumbers);
        mav.addObject("bracketMatchesByRound", matchesByRound);
        mav.addObject("bracketRoundCount", matchesByRound.size());
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

    private BracketPublishForm createBracketPublishForm(final TournamentBracketView bracketView) {
        final BracketPublishForm form = new BracketPublishForm();
        final List<BracketPublishScheduleForm> schedules = new ArrayList<>();
        final Map<Integer, List<TournamentMatch>> matchesByRound =
                matchesByRound(bracketView.getMatches());
        for (final Map.Entry<Integer, List<TournamentMatch>> round : matchesByRound.entrySet()) {
            for (final TournamentMatch match : round.getValue()) {
                final BracketPublishScheduleForm schedule = new BracketPublishScheduleForm();
                schedule.setMatchId(match.getId());
                schedule.setRoundNumber(round.getKey());
                schedule.setMatchNumber(match.getMatchIndex() + 1);
                schedule.setStartDate(
                        match.getScheduledStartsAt() == null
                                ? defaultScheduleDate()
                                : scheduleDate(match.getScheduledStartsAtDateTime()));
                schedule.setStartTime(
                        match.getScheduledStartsAt() == null
                                ? defaultScheduleStartTime(round.getKey())
                                : scheduleTime(match.getScheduledStartsAtDateTime()));
                schedule.setEndDate(
                        match.getScheduledEndsAt() == null
                                ? defaultScheduleDate()
                                : scheduleDate(match.getScheduledEndsAtDateTime()));
                schedule.setEndTime(
                        match.getScheduledEndsAt() == null
                                ? null
                                : scheduleTime(match.getScheduledEndsAtDateTime()));
                schedule.setAddress(scheduleAddress(bracketView.getTournament(), match));
                schedule.setLatitude(scheduleLatitude(bracketView.getTournament(), match));
                schedule.setLongitude(scheduleLongitude(bracketView.getTournament(), match));
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
                "page.title.hostTournamentCreate",
                null,
                "tournament.create.title",
                "tournament.create.description",
                "/host/tournaments",
                "tournament.form.submit.create",
                "tournament.form.submit.creating",
                false,
                null);
    }

    private TournamentFormConfig editFormConfig(final Tournament tournament, final Locale locale) {
        return new TournamentFormConfig(
                "page.title.hostTournamentEdit",
                tournament.getTitle(),
                "tournament.edit.title",
                "tournament.edit.description",
                "/host/tournaments/" + tournament.getId() + "/edit",
                "tournament.form.submit.edit",
                "tournament.form.submit.saving",
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

    private static Map<Long, List<String>> membersByTeamId(
            final List<TournamentTeamMember> teamMembers) {
        final Map<Long, List<String>> usernamesByTeamId = new LinkedHashMap<>();
        for (final TournamentTeamMember member :
                teamMembers == null ? List.<TournamentTeamMember>of() : teamMembers) {
            if (member.getTeam() == null || member.getUser() == null) {
                continue;
            }
            usernamesByTeamId
                    .computeIfAbsent(member.getTeam().getId(), ignored -> new ArrayList<>())
                    .add(member.getUser().getUsername());
        }
        return usernamesByTeamId;
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

    private static Map<Integer, List<TournamentMatch>> matchesByRound(
            final List<TournamentMatch> matches) {
        return (matches == null ? List.<TournamentMatch>of() : matches)
                .stream()
                        .sorted(
                                Comparator.comparingInt(TournamentMatch::getRoundNumber)
                                        .thenComparingInt(TournamentMatch::getMatchIndex))
                        .collect(
                                Collectors.groupingBy(
                                        TournamentMatch::getRoundNumber,
                                        LinkedHashMap::new,
                                        Collectors.toList()));
    }

    private static LocalTime defaultScheduleStartTime(final int roundNumber) {
        return LocalTime.of(18, 0).plusHours(Math.max(0, roundNumber - 1));
    }

    private static LocalDate defaultScheduleDate() {
        return LocalDate.now(PlatformTime.ZONE).plusDays(1);
    }

    private static LocalDate scheduleDate(final OffsetDateTime dateTime) {
        return dateTime == null ? null : dateTime.toLocalDate();
    }

    private static LocalTime scheduleTime(final OffsetDateTime dateTime) {
        return dateTime == null ? null : dateTime.toLocalTime();
    }

    private static String scheduleAddress(
            final Tournament tournament, final TournamentMatch match) {
        return normalizeBlank(match.getAddress()).isEmpty()
                ? tournament.getAddress()
                : match.getAddress();
    }

    private static Double scheduleLatitude(
            final Tournament tournament, final TournamentMatch match) {
        return match.getLatitude() == null ? tournament.getLatitude() : match.getLatitude();
    }

    private static Double scheduleLongitude(
            final Tournament tournament, final TournamentMatch match) {
        return match.getLongitude() == null ? tournament.getLongitude() : match.getLongitude();
    }

    private static Optional<String> flashString(final Model model, final String name) {
        final Object value = model.asMap().get(name);
        return value instanceof String ? Optional.of((String) value) : Optional.empty();
    }

    private static String normalizeBlank(final String value) {
        return value == null ? "" : value.trim();
    }

    private record TournamentFormConfig(
            String pageTitleCode,
            String pageTitleArgument,
            String titleCode,
            String descriptionCode,
            String action,
            String submitLabelCode,
            String submitLoadingLabelCode,
            boolean editMode,
            String bannerImageUrl) {}
}
