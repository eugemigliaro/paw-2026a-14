package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.TournamentSoloEntry;
import ar.edu.itba.paw.models.TournamentTeam;
import ar.edu.itba.paw.models.TournamentTeamMember;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.exceptions.tournament.TournamentForbiddenActionException;
import ar.edu.itba.paw.models.exceptions.tournament.TournamentNotFoundException;
import ar.edu.itba.paw.models.exceptions.tournamentRegistration.*;
import ar.edu.itba.paw.models.types.TournamentPairingStrategy;
import ar.edu.itba.paw.models.types.TournamentSoloEntryStatus;
import ar.edu.itba.paw.models.types.TournamentStatus;
import ar.edu.itba.paw.models.types.TournamentTeamOrigin;
import ar.edu.itba.paw.persistence.TournamentSoloEntryDao;
import ar.edu.itba.paw.services.internal.TournamentDataService;
import ar.edu.itba.paw.services.internal.TournamentTeamDataService;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TournamentRegistrationServiceImpl implements TournamentRegistrationService {

    private final SecurityService securityService;
    private final TournamentDataService tournamentDataService;
    private final TournamentSoloEntryDao tournamentSoloEntryDao;
    private final TournamentTeamDataService tournamentTeamDataService;
    private final Clock clock;

    public TournamentRegistrationServiceImpl(
            final TournamentDataService tournamentDataService,
            final TournamentSoloEntryDao tournamentSoloEntryDao,
            final SecurityService securityService,
            final TournamentTeamDataService tournamentTeamDataService,
            final Clock clock) {
        this.tournamentDataService = tournamentDataService;
        this.tournamentSoloEntryDao = tournamentSoloEntryDao;
        this.securityService = securityService;
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
            throw new TournamentRegistrationSoloSignupDisabledException();
        }

        if (tournamentTeamDataService.findUserTeam(tournamentId, user.getId()).isPresent()) {
            throw new TournamentRegistrationAlreadyOnTeamException();
        }

        final Optional<TournamentSoloEntry> existing =
                tournamentSoloEntryDao.findByTournamentAndUser(tournamentId, user.getId());
        if (existing.isPresent()) {
            final TournamentSoloEntry soloEntry = existing.get();
            if (TournamentSoloEntryStatus.IN_POOL == soloEntry.getStatus()) {
                return soloEntry;
            }
            if (TournamentSoloEntryStatus.ASSIGNED == soloEntry.getStatus()) {
                throw new TournamentRegistrationAlreadyAssignedException();
            }
        }

        if (isSoloPoolFull(tournament)) {
            throw new TournamentRegistrationSoloPoolFullException();
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
                        .orElseThrow(() -> new TournamentRegistrationNotInSoloPoolException());

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
    public TournamentRegistrationState getRegistrationState(
            final Tournament tournament, final User user, final boolean canCloseRegistration) {
        final Optional<TournamentSoloEntry> soloEntry = findSoloEntry(tournament, user);
        final Optional<TournamentTeam> userTeam = findUserTeam(tournament, user);
        final Instant now = Instant.now(clock);
        final boolean registrationOpen = isRegistrationOpenNow(tournament, now);
        final boolean registrationNotStarted = isRegistrationNotStarted(tournament, now);
        final TournamentSoloEntryStatus soloStatus =
                soloEntry.map(TournamentSoloEntry::getStatus).orElse(null);
        final boolean canJoinSolo =
                user != null
                        && user.getId() != null
                        && registrationOpen
                        && tournament.isAllowSoloSignup()
                        && !isSoloPoolFull(tournament)
                        && userTeam.isEmpty()
                        && soloStatus != TournamentSoloEntryStatus.IN_POOL
                        && soloStatus != TournamentSoloEntryStatus.ASSIGNED;
        final boolean canLeaveSolo =
                user != null
                        && user.getId() != null
                        && registrationOpen
                        && soloStatus == TournamentSoloEntryStatus.IN_POOL;
        final boolean requiresLoginToJoin =
                user == null && registrationOpen && tournament.isAllowSoloSignup();
        final TournamentRegistrationReadiness readiness =
                canCloseRegistration ? registrationReadiness(tournament) : null;
        final boolean closeRegistrationBlocked =
                readiness != null && readiness.isCancellationRisk();
        final boolean closeRegistrationDisabled = !registrationOpen || closeRegistrationBlocked;
        return new TournamentRegistrationState(
                soloEntry,
                userTeam,
                readiness,
                registrationOpen,
                registrationNotStarted,
                canJoinSolo,
                canLeaveSolo,
                requiresLoginToJoin,
                closeRegistrationDisabled);
    }

    @Override
    public TournamentRegistrationReadiness getRegistrationReadiness(
            final long tournamentId, final User actingUser) {
        final Tournament tournament = findTournamentOrThrow(tournamentId);
        validateCanMutate(tournament, actingUser);
        if (TournamentStatus.REGISTRATION != tournament.getStatus()) {
            return new TournamentRegistrationReadiness(0, 0, 0, false);
        }

        return registrationReadiness(tournament);
    }

    private TournamentRegistrationReadiness registrationReadiness(final Tournament tournament) {
        final long tournamentId = tournament.getId();
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
            throw new TournamentRegistrationUnderCapacityException();
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
                .orElseThrow(() -> new TournamentNotFoundException());
    }

    private void requireRegistrationOpen(final Tournament tournament) {
        if (TournamentStatus.REGISTRATION != tournament.getStatus()
                || !isRegistrationOpenNow(tournament)) {
            throw new TournamentRegistrationNotOpenException();
        }
    }

    private boolean isRegistrationOpenNow(final Tournament tournament) {
        final Instant now = Instant.now(clock);
        return isRegistrationOpenNow(tournament, now);
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

    private void validateCanMutate(final Tournament tournament, final User actingUser) {
        if (!canMutate(tournament, actingUser)) {
            throw new TournamentForbiddenActionException();
        }
    }

    private boolean canMutate(final Tournament tournament, final User actingUser) {
        if (tournament == null || actingUser == null || actingUser.getId() == null) {
            return false;
        }
        return tournament.getHost().getId().equals(actingUser.getId())
                || securityService.canActAsAdminMod(actingUser);
    }

    private Optional<TournamentSoloEntry> findSoloEntry(
            final Tournament tournament, final User user) {
        if (tournament == null
                || tournament.getId() == null
                || user == null
                || user.getId() == null) {
            return Optional.empty();
        }
        return tournamentSoloEntryDao.findByTournamentAndUser(tournament.getId(), user.getId());
    }

    private Optional<TournamentTeam> findUserTeam(final Tournament tournament, final User user) {
        if (tournament == null
                || tournament.getId() == null
                || user == null
                || user.getId() == null) {
            return Optional.empty();
        }
        return tournamentTeamDataService.findUserTeam(tournament.getId(), user.getId());
    }

    private void validateUser(final User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("exception.user.notNull");
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
