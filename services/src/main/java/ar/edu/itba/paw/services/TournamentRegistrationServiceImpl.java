package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.TournamentSoloEntry;
import ar.edu.itba.paw.models.TournamentTeam;
import ar.edu.itba.paw.models.TournamentTeamMember;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.types.TournamentPairingStrategy;
import ar.edu.itba.paw.models.types.TournamentSoloEntryStatus;
import ar.edu.itba.paw.models.types.TournamentStatus;
import ar.edu.itba.paw.models.types.TournamentTeamOrigin;
import ar.edu.itba.paw.persistence.TournamentSoloEntryDao;
import ar.edu.itba.paw.services.exceptions.tournamentRegistration.*;
import ar.edu.itba.paw.services.internal.TournamentDataService;
import ar.edu.itba.paw.services.internal.TournamentTeamDataService;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TournamentRegistrationServiceImpl implements TournamentRegistrationService {

    private static final String ADMIN_MOD_AUTHORITY = "ROLE_ADMIN_MOD";

    private final TournamentDataService tournamentDataService;
    private final TournamentSoloEntryDao tournamentSoloEntryDao;
    private final TournamentTeamDataService tournamentTeamDataService;
    private final Clock clock;

    public TournamentRegistrationServiceImpl(
            final TournamentDataService tournamentDataService,
            final TournamentSoloEntryDao tournamentSoloEntryDao,
            final TournamentTeamDataService tournamentTeamDataService,
            final Clock clock) {
        this.tournamentDataService = tournamentDataService;
        this.tournamentSoloEntryDao = tournamentSoloEntryDao;
        this.tournamentTeamDataService = tournamentTeamDataService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public TournamentSoloEntry joinSolo(final long tournamentId, final User user) {
        validateUser(user);
        final Tournament tournament = findTournamentOrThrow(tournamentId);
        requireRegistrationOpen(tournament);

        if (!tournament.isAllowSoloSignup()) {
            throw new TournamentRegistrationSoloSignupDisabledException(
                    "Solo signup is disabled for this tournament");
        }

        if (tournamentTeamDataService.findUserTeam(tournamentId, user.getId()).isPresent()) {
            throw new TournamentRegistrationAlreadyOnTeamException(
                    "User is already on a team in this tournament");
        }

        final Optional<TournamentSoloEntry> existing =
                tournamentSoloEntryDao.findByTournamentAndUser(tournamentId, user.getId());
        if (existing.isPresent()) {
            final TournamentSoloEntry soloEntry = existing.get();
            if (TournamentSoloEntryStatus.IN_POOL == soloEntry.getStatus()) {
                return soloEntry;
            }
            if (TournamentSoloEntryStatus.ASSIGNED == soloEntry.getStatus()) {
                throw new TournamentRegistrationAlreadyAssignedException(
                        "User is already assigned to a team in this tournament");
            }
        }

        if (isSoloPoolFull(tournament)) {
            throw new TournamentRegistrationSoloPoolFullException(
                    "Solo pool is full for this tournament");
        }

        if (existing.isEmpty()) {
            return tournamentSoloEntryDao.create(
                    tournament, user, TournamentSoloEntryStatus.IN_POOL);
        }

        final TournamentSoloEntry soloEntry = existing.get();
        soloEntry.setStatus(TournamentSoloEntryStatus.IN_POOL);
        soloEntry.setAssignedTeam(null);
        soloEntry.setJoinedAt(Instant.now(clock));
        soloEntry.setLeftAt(null);
        return tournamentSoloEntryDao.update(soloEntry);
    }

    @Override
    @Transactional
    public void leaveSolo(final long tournamentId, final User user) {
        validateUser(user);
        final Tournament tournament = findTournamentOrThrow(tournamentId);
        requireRegistrationOpen(tournament);

        final TournamentSoloEntry soloEntry =
                tournamentSoloEntryDao
                        .findByTournamentAndUser(tournamentId, user.getId())
                        .filter(entry -> TournamentSoloEntryStatus.IN_POOL == entry.getStatus())
                        .orElseThrow(
                                () ->
                                        new TournamentRegistrationNotInSoloPoolException(
                                                "User is not in the solo pool for this tournament"));

        soloEntry.setStatus(TournamentSoloEntryStatus.LEFT);
        soloEntry.setAssignedTeam(null);
        soloEntry.setLeftAt(Instant.now(clock));
        tournamentSoloEntryDao.update(soloEntry);
    }

    @Override
    public boolean isSoloPoolFull(final long tournamentId) {
        final Tournament tournament = findTournamentOrThrow(tournamentId);
        return isSoloPoolFull(tournament);
    }

    @Override
    public Optional<TournamentSoloEntry> findSoloEntry(final long tournamentId, final User user) {
        if (user == null || user.getId() == null) {
            return Optional.empty();
        }
        return tournamentSoloEntryDao.findByTournamentAndUser(tournamentId, user.getId());
    }

    @Override
    public Optional<TournamentTeam> findUserTeam(final long tournamentId, final User user) {
        if (user == null || user.getId() == null) {
            return Optional.empty();
        }
        return tournamentTeamDataService.findUserTeam(tournamentId, user.getId());
    }

    @Override
    public List<TournamentSoloEntry> listActiveSoloEntries(final long tournamentId) {
        final Tournament tournament = findTournamentOrThrow(tournamentId);
        if (TournamentStatus.REGISTRATION != tournament.getStatus()) {
            return List.of();
        }
        return tournamentSoloEntryDao.findActiveByTournament(tournamentId);
    }

    @Override
    public List<TournamentTeamMember> listTeamMembers(final long tournamentId) {
        findTournamentOrThrow(tournamentId);
        return tournamentTeamDataService.findMembersByTournament(tournamentId);
    }

    @Override
    public TournamentRegistrationReadiness getRegistrationReadiness(
            final long tournamentId, final User actingUser) {
        final Tournament tournament = findTournamentOrThrow(tournamentId);
        validateCanMutate(tournament, actingUser);
        if (TournamentStatus.REGISTRATION != tournament.getStatus()) {
            return new TournamentRegistrationReadiness(0, 0, 0, false);
        }

        final int activeSoloEntries =
                Math.toIntExact(tournamentSoloEntryDao.countActiveByTournament(tournamentId));
        final int existingTeamCount =
                Math.toIntExact(tournamentTeamDataService.countByTournament(tournamentId));
        final int availableTeamSlots = Math.max(0, tournament.getBracketSize() - existingTeamCount);
        final int soloTeamCount =
                Math.min(activeSoloEntries / tournament.getTeamSize(), availableTeamSlots);
        final int finalTeamCount = existingTeamCount + soloTeamCount;
        return new TournamentRegistrationReadiness(
                activeSoloEntries, existingTeamCount, finalTeamCount, finalTeamCount < 2);
    }

    @Override
    @Transactional
    public Tournament closeRegistration(final long tournamentId, final User actingUser) {
        final Tournament tournament = findTournamentOrThrow(tournamentId);
        validateCanMutate(tournament, actingUser);
        requireRegistrationOpen(tournament);

        final List<TournamentSoloEntry> activeEntries =
                tournamentSoloEntryDao.findActiveByTournament(tournamentId);
        final int existingTeamCount =
                Math.toIntExact(tournamentTeamDataService.countByTournament(tournamentId));
        final int availableTeamSlots = Math.max(0, tournament.getBracketSize() - existingTeamCount);
        final int soloTeamCount =
                Math.min(activeEntries.size() / tournament.getTeamSize(), availableTeamSlots);

        final int finalTeamCount = existingTeamCount + soloTeamCount;
        if (finalTeamCount < 2) {
            throw new TournamentRegistrationUnderCapacityException(
                    "Tournament does not have enough teams to start");
        }

        final int assignableEntries = soloTeamCount * tournament.getTeamSize();
        for (int teamIndex = 0; teamIndex < soloTeamCount; teamIndex++) {
            final TournamentTeam team =
                    tournamentTeamDataService.create(
                            tournament, null, TournamentTeamOrigin.SOLO_POOL, null);

            final int firstEntryIndex = teamIndex * tournament.getTeamSize();
            for (int playerOffset = 0; playerOffset < tournament.getTeamSize(); playerOffset++) {
                final TournamentSoloEntry soloEntry =
                        activeEntries.get(firstEntryIndex + playerOffset);
                tournamentTeamDataService.addMember(team, soloEntry.getUser(), false);
                soloEntry.setStatus(TournamentSoloEntryStatus.ASSIGNED);
                soloEntry.setAssignedTeam(team);
                tournamentSoloEntryDao.update(soloEntry);
            }
        }

        markUnassigned(activeEntries.subList(assignableEntries, activeEntries.size()));

        final Instant now = Instant.now(clock);
        tournament.setPairingStrategy(TournamentPairingStrategy.RANDOM);
        tournament.setStatus(TournamentStatus.BRACKET_SETUP);
        tournament.setRegistrationClosedAt(now);
        tournament.setUpdatedAt(now);
        return tournamentDataService.update(tournament);
    }

    private Tournament findTournamentOrThrow(final long tournamentId) {
        return tournamentDataService
                .findById(tournamentId)
                .filter(tournament -> !tournament.isDeleted())
                .orElseThrow(
                        () ->
                                new TournamentRegistrationTournamentNotFoundException(
                                        "Tournament not found"));
    }

    private void requireRegistrationOpen(final Tournament tournament) {
        if (TournamentStatus.REGISTRATION != tournament.getStatus()
                || !isRegistrationOpenNow(tournament)) {
            throw new TournamentRegistrationNotOpenException(
                    "Registration is not open for this tournament");
        }
    }

    private boolean isRegistrationOpenNow(final Tournament tournament) {
        final Instant now = Instant.now(clock);
        final Instant opensAt = tournament.getRegistrationOpensAt();
        final Instant closesAt = tournament.getRegistrationClosesAt();
        return opensAt != null
                && closesAt != null
                && !now.isBefore(opensAt)
                && now.isBefore(closesAt);
    }

    private void validateCanMutate(final Tournament tournament, final User actingUser) {
        if (!canMutate(tournament, actingUser)) {
            throw new TournamentRegistrationForbiddenException(
                    "You are not allowed to modify this tournament");
        }
    }

    private boolean canMutate(final Tournament tournament, final User actingUser) {
        if (tournament == null || actingUser == null || actingUser.getId() == null) {
            return false;
        }
        return tournament.getHost().getId().equals(actingUser.getId()) || isAdminMod();
    }

    private boolean isAdminMod() {
        final Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .filter(Objects::nonNull)
                .map(GrantedAuthority::getAuthority)
                .anyMatch(ADMIN_MOD_AUTHORITY::equals);
    }

    private void validateUser(final User user) {
        if (user == null || user.getId() == null) {
            throw new TournamentRegistrationInvalidUserException(
                    "User must be authenticated to perform this action");
        }
    }

    private void markUnassigned(final List<TournamentSoloEntry> soloEntries) {
        for (final TournamentSoloEntry soloEntry : soloEntries) {
            soloEntry.setStatus(TournamentSoloEntryStatus.UNASSIGNED);
            soloEntry.setAssignedTeam(null);
            tournamentSoloEntryDao.update(soloEntry);
        }
    }

    private boolean isSoloPoolFull(final Tournament tournament) {
        final long currentSoloEntries =
                tournamentSoloEntryDao.countActiveByTournament(tournament.getId());
        final long maxSoloEntries = (long) tournament.getBracketSize() * tournament.getTeamSize();
        return currentSoloEntries >= maxSoloEntries;
    }
}
