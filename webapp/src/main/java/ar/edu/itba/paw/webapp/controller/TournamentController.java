package ar.edu.itba.paw.webapp.controller;

import static ar.edu.itba.paw.webapp.utils.ImageUrlHelper.bannerUrlFor;
import static ar.edu.itba.paw.webapp.utils.ImageUrlHelper.profileUrlFor;

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
import ar.edu.itba.paw.webapp.form.CreateTournamentTeamForm;
import ar.edu.itba.paw.webapp.security.annotation.AuthenticatedUser;
import ar.edu.itba.paw.webapp.security.annotation.CurrentUser;
import ar.edu.itba.paw.webapp.utils.EventCardAttributeUtils;
import ar.edu.itba.paw.webapp.viewmodel.TournamentTeamRosterView;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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
public class TournamentController {
    private static final int DEFAULT_MAP_ZOOM = 14;

    private final TournamentService tournamentService;
    private final TournamentRegistrationService tournamentRegistrationService;
    private final TournamentBracketService tournamentBracketService;
    private final boolean mapPickerEnabled;
    private final String mapTileUrlTemplate;
    private final String mapAttribution;
    private final int mapDefaultZoom;

    @Autowired
    public TournamentController(
            final TournamentService tournamentService,
            final TournamentRegistrationService tournamentRegistrationService,
            final TournamentBracketService tournamentBracketService,
            @Value("${map.picker.enabled:false}") final boolean mapPickerEnabled,
            @Value("${map.tiles.urlTemplate:}") final String mapTileUrlTemplate,
            @Value("${map.tiles.attribution:}") final String mapAttribution,
            @Value("${map.default.zoom:" + DEFAULT_MAP_ZOOM + "}") final int mapDefaultZoom) {
        this.tournamentService = tournamentService;
        this.tournamentRegistrationService = tournamentRegistrationService;
        this.tournamentBracketService = tournamentBracketService;
        this.mapPickerEnabled = mapPickerEnabled;
        this.mapTileUrlTemplate = mapTileUrlTemplate;
        this.mapAttribution = mapAttribution;
        this.mapDefaultZoom = mapDefaultZoom;
    }

    @GetMapping("/tournaments/{tournamentId:\\d+}")
    public ModelAndView showTournament(
            @CurrentUser final User user,
            @PathVariable("tournamentId") final Long tournamentId,
            final Model model) {
        final Tournament tournament =
                tournamentService
                        .findPublicTournament(tournamentId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        final Optional<TournamentSoloEntry> soloEntry =
                tournamentRegistrationService.findSoloEntry(tournamentId, user);
        final Optional<TournamentTeam> userTeam =
                tournamentRegistrationService.findUserTeam(tournamentId, user);

        final ModelAndView mav = new ModelAndView("tournaments/detail");
        addTournamentDetailModel(
                mav,
                tournament,
                user,
                soloEntry,
                userTeam,
                tournamentService.findParticipatingTournamentIds(user, List.of(tournamentId)));
        mav.addObject("soloJoinPath", "/tournaments/" + tournamentId + "/solo-entry");
        mav.addObject("soloLeavePath", "/tournaments/" + tournamentId + "/solo-entry/leave");
        mav.addObject("teamCreatePath", "/tournaments/" + tournamentId + "/teams");
        mav.addObject("teamLeavePath", "/tournaments/" + tournamentId + "/teams/leave");
        mav.addObject(
                "closeRegistrationPath",
                "/host/tournaments/" + tournamentId + "/close-registration");
        mav.addObject("editTournamentPath", "/host/tournaments/" + tournamentId + "/edit");
        mav.addObject("cancelTournamentPath", "/host/tournaments/" + tournamentId + "/cancel");
        mav.addObject(
                "tournamentNoticeCode", flashString(model, "tournamentNoticeCode").orElse(null));
        mav.addObject(
                "tournamentErrorCode", flashString(model, "tournamentErrorCode").orElse(null));
        mav.addObject(
                "mapAvailable",
                mapPickerEnabled && !mapTileUrlTemplate.isBlank() && tournament.hasCoordinates());
        mav.addObject("mapLatitude", tournament.getLatitude());
        mav.addObject("mapLongitude", tournament.getLongitude());
        mav.addObject("mapTileUrlTemplate", mapTileUrlTemplate);
        mav.addObject("mapAttribution", mapAttribution);
        mav.addObject("mapZoom", mapDefaultZoom);
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
            final Model model) {
        TournamentBracketView bracketView;
        try {
            bracketView = tournamentBracketService.getBracket(tournamentId, user);
        } catch (final TournamentBracketNotGeneratedException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }

        final ModelAndView mav = new ModelAndView("tournaments/bracket");
        addBracketModel(mav, user, bracketView);
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

    @PostMapping("/tournaments/{tournamentId:\\d+}/teams")
    public ModelAndView createTeam(
            @AuthenticatedUser final User user,
            @PathVariable("tournamentId") final Long tournamentId,
            @Valid @ModelAttribute("createTournamentTeamForm") final CreateTournamentTeamForm form,
            final BindingResult bindingResult,
            final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "tournamentErrorCode", "tournament.registration.error.teamNameRequired");
            return new ModelAndView("redirect:/tournaments/" + tournamentId);
        }
        try {
            tournamentRegistrationService.createTeam(tournamentId, user, form.getName());
        } catch (final TournamentRegistrationException e) {
            final String errorCode = "tournament.registration.error." + e.getMessage();
            redirectAttributes.addFlashAttribute("tournamentErrorCode", errorCode);
        }
        return new ModelAndView("redirect:/tournaments/" + tournamentId);
    }

    @PostMapping("/tournaments/{tournamentId:\\d+}/teams/{teamId:\\d+}/join")
    public ModelAndView joinTeam(
            @AuthenticatedUser final User user,
            @PathVariable("tournamentId") final Long tournamentId,
            @PathVariable("teamId") final Long teamId,
            final RedirectAttributes redirectAttributes) {
        try {
            tournamentRegistrationService.joinTeam(tournamentId, teamId, user);
        } catch (final TournamentRegistrationException e) {
            final String errorCode = "tournament.registration.error." + e.getMessage();
            redirectAttributes.addFlashAttribute("tournamentErrorCode", errorCode);
        }
        return new ModelAndView("redirect:/tournaments/" + tournamentId);
    }

    @PostMapping("/tournaments/{tournamentId:\\d+}/teams/leave")
    public ModelAndView leaveTeam(
            @AuthenticatedUser final User user,
            @PathVariable("tournamentId") final Long tournamentId,
            final RedirectAttributes redirectAttributes) {
        try {
            tournamentRegistrationService.leaveTeam(tournamentId, user);
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
            final Set<Long> participatingTournamentIds) {
        final TournamentViewerCapabilities capabilities =
                tournamentService.viewerCapabilities(tournament, currentUser);
        final List<TournamentTeamMember> teamMembers =
                tournamentRegistrationService.listTeamMembers(tournament.getId());
        final List<TournamentSoloEntry> activeSoloEntries =
                tournamentRegistrationService.listActiveSoloEntries(tournament.getId());
        final Map<Long, Integer> teamDisplayNumbers = teamDisplayNumbersFromMembers(teamMembers);

        mav.addObject("tournament", tournament);
        mav.addObject("tournamentCapabilities", capabilities);
        mav.addObject("tournamentJoinModeCode", joinModeCode(tournament));
        mav.addObject("tournamentHostProfileHref", hostProfileHref(tournament.getHost()));
        mav.addObject("tournamentHostUsername", hostUsername(tournament.getHost()));
        mav.addObject("tournamentUnknownHostArgument", unknownHostArgument(tournament.getHost()));
        mav.addObject("tournamentHostProfileImageUrl", profileUrlFor(tournament.getHost()));
        mav.addObject("tournamentBannerImageUrl", bannerUrlFor(tournament));
        mav.addObject(
                "tournamentRelationshipBadgeCodes",
                EventCardAttributeUtils.tournamentRelationshipBadgeCodes(
                                List.of(tournament), currentUser, participatingTournamentIds)
                        .getOrDefault(tournament.getId(), List.of()));
        addParticipationModel(mav, soloEntry, userTeam);
        mav.addObject("tournamentNextStepCode", nextStepCode(tournament));
        mav.addObject("tournamentAboutParagraphs", aboutParagraphs(tournament));
        mav.addObject("tournamentTeamMembers", teamMembers);
        mav.addObject("tournamentActiveSoloEntries", activeSoloEntries);
        mav.addObject("tournamentTeamDisplayNumbers", teamDisplayNumbers);
        mav.addObject("tournamentTeamRosters", teamRosters(teamMembers, tournament.getTeamSize()));
        mav.addObject("userProfileImageUrls", userProfileImageUrls(teamMembers, activeSoloEntries));
        mav.addObject("tournamentUserTeamId", userTeam.map(TournamentTeam::getId).orElse(null));
        mav.addObject(
                "tournamentCloseRegistrationDisabledMessage",
                closeRegistrationDisabledMessageCode(capabilities));
    }

    private static List<TournamentTeamRosterView> teamRosters(
            final List<TournamentTeamMember> teamMembers, final int teamSize) {
        final Map<Long, List<TournamentTeamMember>> membersByTeamId = new LinkedHashMap<>();
        final Map<Long, TournamentTeam> teamsById = new LinkedHashMap<>();
        for (final TournamentTeamMember member : teamMembers) {
            final TournamentTeam team = member.getTeam();
            if (team == null || team.getId() == null) {
                continue;
            }
            teamsById.putIfAbsent(team.getId(), team);
            membersByTeamId.computeIfAbsent(team.getId(), ignored -> new ArrayList<>()).add(member);
        }
        final List<TournamentTeamRosterView> rosters = new ArrayList<>();
        for (final Map.Entry<Long, TournamentTeam> entry : teamsById.entrySet()) {
            rosters.add(
                    new TournamentTeamRosterView(
                            entry.getValue(), membersByTeamId.get(entry.getKey()), teamSize));
        }
        return rosters;
    }

    private static Map<Long, String> userProfileImageUrls(
            final List<TournamentTeamMember> teamMembers,
            final List<TournamentSoloEntry> soloEntries) {
        final Map<Long, String> urls = new LinkedHashMap<>();
        for (final TournamentTeamMember member : teamMembers) {
            final User user = member.getUser();
            if (user != null && user.getId() != null) {
                urls.putIfAbsent(user.getId(), profileUrlFor(user));
            }
        }
        for (final TournamentSoloEntry entry : soloEntries) {
            final User user = entry.getUser();
            if (user != null && user.getId() != null) {
                urls.putIfAbsent(user.getId(), profileUrlFor(user));
            }
        }
        return urls;
    }

    private static String closeRegistrationDisabledMessageCode(
            final TournamentViewerCapabilities capabilities) {
        if (!capabilities.isCloseRegistrationBlockedByCapacity()) {
            return null;
        }
        return "tournament.host.closeRegistration.unavailable";
    }

    private static String joinModeCode(final Tournament tournament) {
        if (tournament.isAllowSoloSignup() && tournament.isAllowTeamDraft()) {
            return "tournament.detail.joinMode.both";
        }
        if (tournament.isAllowSoloSignup()) {
            return "tournament.detail.joinMode.solo";
        }
        if (tournament.isAllowTeamDraft()) {
            return "tournament.detail.joinMode.teamDraft";
        }
        return "tournament.detail.joinMode.closed";
    }

    private static void addParticipationModel(
            final ModelAndView mav,
            final Optional<TournamentSoloEntry> soloEntry,
            final Optional<TournamentTeam> userTeam) {
        if (userTeam.isPresent()) {
            mav.addObject("tournamentParticipationCode", "tournament.participation.team");
            mav.addObject("tournamentParticipationTeam", userTeam.get());
            return;
        }
        if (soloEntry.isEmpty()) {
            return;
        }
        final TournamentSoloEntry entry = soloEntry.get();
        if (TournamentSoloEntryStatus.IN_POOL == entry.getStatus()) {
            mav.addObject("tournamentParticipationCode", "tournament.participation.soloPool");
            return;
        }
        if (TournamentSoloEntryStatus.ASSIGNED == entry.getStatus()) {
            final TournamentTeam assignedTeam = entry.getAssignedTeam();
            mav.addObject(
                    "tournamentParticipationCode",
                    assignedTeam == null
                            ? "tournament.participation.assigned"
                            : "tournament.participation.assignedTeam");
            mav.addObject("tournamentParticipationTeam", assignedTeam);
            return;
        }
        if (TournamentSoloEntryStatus.UNASSIGNED == entry.getStatus()) {
            mav.addObject("tournamentParticipationCode", "tournament.participation.unassigned");
            return;
        }
        if (TournamentSoloEntryStatus.LEFT == entry.getStatus()) {
            mav.addObject("tournamentParticipationCode", "tournament.participation.left");
        }
    }

    private static String nextStepCode(final Tournament tournament) {
        if (TournamentStatus.BRACKET_SETUP == tournament.getStatus()) {
            return "tournament.nextStep.bracketSetup";
        }
        if (TournamentStatus.CANCELLED == tournament.getStatus()) {
            return "tournament.nextStep.cancelled";
        }
        if (TournamentStatus.IN_PROGRESS == tournament.getStatus()) {
            return "tournament.nextStep.inProgress";
        }
        if (TournamentStatus.COMPLETED == tournament.getStatus()) {
            return "tournament.nextStep.completed";
        }
        return null;
    }

    private static List<String> aboutParagraphs(final Tournament tournament) {
        final String description = tournament.getDescription();
        if (description == null || description.isBlank()) {
            return List.of();
        }
        return List.of(description.split("\\R+")).stream()
                .map(String::trim)
                .filter(paragraph -> !paragraph.isEmpty())
                .collect(Collectors.toList());
    }

    private String hostProfileHref(final User host) {
        if (host == null || host.getUsername() == null || host.getUsername().isBlank()) {
            return null;
        }
        return "/users/" + host.getUsername();
    }

    private static String hostUsername(final User host) {
        if (host == null || host.getUsername() == null || host.getUsername().isBlank()) {
            return null;
        }
        return host.getUsername();
    }

    private static String unknownHostArgument(final User host) {
        return host == null || host.getId() == null ? "?" : String.valueOf(host.getId());
    }

    private void addBracketModel(
            final ModelAndView mav,
            final User currentUser,
            final TournamentBracketView bracketView) {
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
