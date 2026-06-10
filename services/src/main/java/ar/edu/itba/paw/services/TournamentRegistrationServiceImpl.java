package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.TournamentSoloEntry;
import ar.edu.itba.paw.models.TournamentTeam;
import ar.edu.itba.paw.models.TournamentTeamMember;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.exceptions.tournament.TournamentNotFoundException;
import ar.edu.itba.paw.models.exceptions.tournamentRegistration.*;
import ar.edu.itba.paw.models.exceptions.user.InvalidUserException;
import ar.edu.itba.paw.models.types.TournamentPairingStrategy;
import ar.edu.itba.paw.models.types.TournamentSoloEntryStatus;
import ar.edu.itba.paw.models.types.TournamentStatus;
import ar.edu.itba.paw.models.types.TournamentTeamOrigin;
import ar.edu.itba.paw.persistence.TournamentSoloEntryDao;
import ar.edu.itba.paw.services.internal.TournamentDataService;
import ar.edu.itba.paw.services.internal.TournamentTeamDataService;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TournamentRegistrationServiceImpl implements TournamentRegistrationService {

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
        // Serialize all registration writes for this tournament so the shared people-capacity
        // check cannot be raced across the solo and team paths (see joinTeam/createTeam).
        tournamentDataService.lockForRegistration(tournamentId);
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
    @Transactional
    public void withdrawFromOpenRegistrations(final User user) {
        validateUser(user);
        final Instant now = Instant.now(clock);

        // removing if in solo entry
        for (final TournamentSoloEntry soloEntry :
                tournamentSoloEntryDao.findInPoolEntriesByUser(user)) {
            soloEntry.setStatus(TournamentSoloEntryStatus.LEFT);
            soloEntry.setAssignedTeam(null);
            soloEntry.setLeftAt(now);
            tournamentSoloEntryDao.update(soloEntry);
        }

        // removing if in a team
        for (final Tournament tournament :
                tournamentTeamDataService.findTournamentsByMember(user)) {
            if (TournamentStatus.REGISTRATION != tournament.getStatus()) {
                continue;
            }
            tournamentDataService.lockForRegistration(tournament.getId());
            tournamentTeamDataService
                    .findUserTeam(tournament.getId(), user.getId())
                    .ifPresent(team -> removeFromTeam(team, user));
        }
    }

    @Override
    @Transactional
    public TournamentTeam createTeam(final long tournamentId, final User user, final String name) {
        validateUser(user);
        final Tournament tournament = findTournamentOrThrow(tournamentId);
        tournamentDataService.lockForRegistration(tournamentId);
        requireRegistrationOpen(tournament);

        if (!tournament.isAllowTeamDraft()) {
            throw new TournamentRegistrationTeamDraftDisabledException();
        }

        final String trimmedName = name == null ? null : name.trim();
        if (trimmedName == null || trimmedName.isEmpty()) {
            throw new TournamentRegistrationTeamNameRequiredException();
        }

        requireNotAlreadyRegistered(tournament, user);

        if (tournamentTeamDataService.countByTournament(tournamentId)
                >= tournament.getBracketSize()) {
            throw new TournamentRegistrationTeamCapReachedException();
        }
        if (isAtCapacity(tournament)) {
            throw new TournamentRegistrationTournamentFullException();
        }
        if (tournamentTeamDataService.existsByTournamentAndName(tournamentId, trimmedName)) {
            throw new TournamentRegistrationTeamNameTakenException();
        }

        final TournamentTeam team =
                tournamentTeamDataService.create(
                        tournament, trimmedName, TournamentTeamOrigin.TEAM_DRAFT, null);
        tournamentTeamDataService.addMember(team, user, false);
        return team;
    }

    @Override
    @Transactional
    public TournamentTeamMember joinTeam(
            final long tournamentId, final long teamId, final User user) {
        validateUser(user);
        final Tournament tournament = findTournamentOrThrow(tournamentId);
        tournamentDataService.lockForRegistration(tournamentId);
        requireRegistrationOpen(tournament);

        if (!tournament.isAllowTeamDraft()) {
            throw new TournamentRegistrationTeamDraftDisabledException();
        }

        requireNotAlreadyRegistered(tournament, user);

        final TournamentTeam team =
                tournamentTeamDataService
                        .findById(teamId)
                        .filter(found -> found.getTournament().getId() == tournamentId)
                        .orElseThrow(TournamentRegistrationTeamNotFoundException::new);

        if (tournamentTeamDataService.countMembers(teamId) >= tournament.getTeamSize()) {
            throw new TournamentRegistrationTeamFullException();
        }
        if (isAtCapacity(tournament)) {
            throw new TournamentRegistrationTournamentFullException();
        }

        return tournamentTeamDataService.addMember(team, user, false);
    }

    @Override
    @Transactional
    public void leaveTeam(final long tournamentId, final User user) {
        validateUser(user);
        final Tournament tournament = findTournamentOrThrow(tournamentId);
        // Serialize against the join paths so the delete-on-empty check below cannot race a
        // concurrent joinTeam: without the lock, this could count zero members and delete the
        // team out from under a player the other transaction just added.
        tournamentDataService.lockForRegistration(tournamentId);
        requireRegistrationOpen(tournament);

        final TournamentTeam team =
                tournamentTeamDataService
                        .findUserTeam(tournamentId, user.getId())
                        .orElseThrow(TournamentRegistrationNotOnTeamException::new);

        removeFromTeam(team, user);
    }

    private void removeFromTeam(final TournamentTeam team, final User user) {
        tournamentTeamDataService.removeMember(team, user);
        if (tournamentTeamDataService.countMembers(team.getId()) == 0) {
            tournamentTeamDataService.delete(team);
        }
    }

    @Override
    public List<TournamentTeam> listJoinableTeams(final long tournamentId) {
        final Tournament tournament = findTournamentOrThrow(tournamentId);
        if (TournamentStatus.REGISTRATION != tournament.getStatus()) {
            return List.of();
        }
        return tournamentTeamDataService.findJoinableByTournament(
                tournamentId, tournament.getTeamSize());
    }

    @Override
    public long countTeams(final long tournamentId) {
        findTournamentOrThrow(tournamentId);
        return tournamentTeamDataService.countByTournament(tournamentId);
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
    public TournamentRegistrationReadiness getRegistrationReadiness(final long tournamentId) {
        final Tournament tournament = findTournamentOrThrow(tournamentId);
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
    public Tournament closeRegistration(final long tournamentId) {
        final Tournament tournament = findTournamentOrThrow(tournamentId);
        // Hold the same lock the join paths take, so no registration write can interleave with
        // team assembly while we read counts and pack teams.
        tournamentDataService.lockForRegistration(tournamentId);
        requireRegistrationOpen(tournament);

        final int teamSize = tournament.getTeamSize();

        // Preserve pre-formed teams that can be completed; top them up from the solo pool.
        topUpIncompleteTeams(tournament, teamSize);
        // Teams still short-handed are dissolved; their members rejoin the solo pool.
        dissolveIncompleteTeams(tournament, teamSize);

        // Pack the remaining solo pool (leftovers + dissolved members) into fresh full teams.
        final List<TournamentSoloEntry> activeEntries =
                tournamentSoloEntryDao.findActiveByTournament(tournamentId);
        final int existingTeamCount =
                Math.toIntExact(tournamentTeamDataService.countByTournament(tournamentId));
        final int availableTeamSlots = Math.max(0, tournament.getBracketSize() - existingTeamCount);
        final int soloTeamCount = Math.min(activeEntries.size() / teamSize, availableTeamSlots);

        final int finalTeamCount = existingTeamCount + soloTeamCount;
        if (finalTeamCount < 2) {
            throw new TournamentRegistrationUnderCapacityException();
        }

        final int assignableEntries = soloTeamCount * teamSize;
        for (int teamIndex = 0; teamIndex < soloTeamCount; teamIndex++) {
            final TournamentTeam team =
                    tournamentTeamDataService.create(
                            tournament, null, TournamentTeamOrigin.SOLO_POOL, null);

            final int firstEntryIndex = teamIndex * teamSize;
            for (int playerOffset = 0; playerOffset < teamSize; playerOffset++) {
                assignSoloEntryToTeam(activeEntries.get(firstEntryIndex + playerOffset), team);
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
            throw new InvalidUserException();
        }
    }

    private void markUnassigned(final List<TournamentSoloEntry> soloEntries) {
        for (final TournamentSoloEntry soloEntry : soloEntries) {
            soloEntry.setStatus(TournamentSoloEntryStatus.UNASSIGNED);
            soloEntry.setAssignedTeam(null);
            tournamentSoloEntryDao.update(soloEntry);
        }
    }

    private void topUpIncompleteTeams(final Tournament tournament, final int teamSize) {
        final long tournamentId = tournament.getId();
        final Map<Long, Integer> memberCounts = memberCountsByTeam(tournamentId);
        final List<TournamentTeam> incompleteTeams =
                tournamentTeamDataService.findByTournamentUnordered(tournamentId).stream()
                        .filter(team -> memberCounts.getOrDefault(team.getId(), 0) < teamSize)
                        .sorted(
                                Comparator.comparingInt(
                                                (TournamentTeam team) ->
                                                        memberCounts.getOrDefault(team.getId(), 0))
                                        .reversed())
                        .toList();
        if (incompleteTeams.isEmpty()) {
            return;
        }

        final Deque<TournamentSoloEntry> pool =
                new ArrayDeque<>(tournamentSoloEntryDao.findActiveByTournament(tournamentId));
        for (final TournamentTeam team : incompleteTeams) {
            int count = memberCounts.getOrDefault(team.getId(), 0);
            while (count < teamSize && !pool.isEmpty()) {
                assignSoloEntryToTeam(pool.poll(), team);
                count++;
            }
        }
    }

    private void dissolveIncompleteTeams(final Tournament tournament, final int teamSize) {
        final long tournamentId = tournament.getId();
        final Map<Long, List<TournamentTeamMember>> membersByTeam = new LinkedHashMap<>();
        for (final TournamentTeamMember member :
                tournamentTeamDataService.findMembersByTournament(tournamentId)) {
            membersByTeam
                    .computeIfAbsent(member.getTeam().getId(), key -> new ArrayList<>())
                    .add(member);
        }

        final Instant now = Instant.now(clock);
        for (final TournamentTeam team :
                tournamentTeamDataService.findByTournamentUnordered(tournamentId)) {
            final List<TournamentTeamMember> members =
                    membersByTeam.getOrDefault(team.getId(), List.of());
            if (members.size() >= teamSize) {
                continue;
            }
            for (final TournamentTeamMember member : members) {
                returnMemberToSoloPool(member.getUser(), tournament, now);
            }
            tournamentTeamDataService.delete(team);
        }
    }

    private Map<Long, Integer> memberCountsByTeam(final long tournamentId) {
        final Map<Long, Integer> memberCounts = new HashMap<>();
        for (final TournamentTeamMember member :
                tournamentTeamDataService.findMembersByTournament(tournamentId)) {
            memberCounts.merge(member.getTeam().getId(), 1, Integer::sum);
        }
        return memberCounts;
    }

    private void assignSoloEntryToTeam(
            final TournamentSoloEntry soloEntry, final TournamentTeam team) {
        tournamentTeamDataService.addMember(team, soloEntry.getUser(), false);
        soloEntry.setStatus(TournamentSoloEntryStatus.ASSIGNED);
        soloEntry.setAssignedTeam(team);
        tournamentSoloEntryDao.update(soloEntry);
    }

    private void returnMemberToSoloPool(
            final User user, final Tournament tournament, final Instant now) {
        final Optional<TournamentSoloEntry> existing =
                tournamentSoloEntryDao.findByTournamentAndUser(tournament.getId(), user.getId());
        if (existing.isEmpty()) {
            tournamentSoloEntryDao.create(tournament, user, TournamentSoloEntryStatus.IN_POOL);
            return;
        }
        final TournamentSoloEntry soloEntry = existing.get();
        soloEntry.setStatus(TournamentSoloEntryStatus.IN_POOL);
        soloEntry.setAssignedTeam(null);
        soloEntry.setJoinedAt(now);
        soloEntry.setLeftAt(null);
        tournamentSoloEntryDao.update(soloEntry);
    }

    private boolean isSoloPoolFull(final Tournament tournament) {
        return isAtCapacity(tournament);
    }

    private boolean isAtCapacity(final Tournament tournament) {
        final long registeredPeople =
                tournamentSoloEntryDao.countActiveByTournament(tournament.getId())
                        + tournamentTeamDataService.countMembersByTournament(tournament.getId());
        final long capacity = (long) tournament.getBracketSize() * tournament.getTeamSize();
        return registeredPeople >= capacity;
    }

    private void requireNotAlreadyRegistered(final Tournament tournament, final User user) {
        if (tournamentTeamDataService.findUserTeam(tournament.getId(), user.getId()).isPresent()) {
            throw new TournamentRegistrationAlreadyOnTeamException();
        }
        final boolean inSoloPool =
                tournamentSoloEntryDao
                        .findByTournamentAndUser(tournament.getId(), user.getId())
                        .filter(entry -> TournamentSoloEntryStatus.IN_POOL == entry.getStatus())
                        .isPresent();
        if (inSoloPool) {
            throw new TournamentRegistrationAlreadyInSoloPoolException();
        }
    }
}
