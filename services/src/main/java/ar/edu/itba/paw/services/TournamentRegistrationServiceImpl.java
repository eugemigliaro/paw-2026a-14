package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.TournamentSoloEntry;
import ar.edu.itba.paw.models.TournamentTeam;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.types.TournamentSoloEntryStatus;
import ar.edu.itba.paw.models.types.TournamentStatus;
import ar.edu.itba.paw.models.types.TournamentTeamOrigin;
import ar.edu.itba.paw.persistence.TournamentDao;
import ar.edu.itba.paw.persistence.TournamentSoloEntryDao;
import ar.edu.itba.paw.persistence.TournamentTeamDao;
import ar.edu.itba.paw.services.exceptions.TournamentRegistrationException;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TournamentRegistrationServiceImpl implements TournamentRegistrationService {

    private static final String ADMIN_MOD_AUTHORITY = "ROLE_ADMIN_MOD";

    private final TournamentDao tournamentDao;
    private final TournamentSoloEntryDao tournamentSoloEntryDao;
    private final TournamentTeamDao tournamentTeamDao;
    private final TournamentMailService tournamentMailService;
    private final MessageSource messageSource;
    private final Clock clock;

    public TournamentRegistrationServiceImpl(
            final TournamentDao tournamentDao,
            final TournamentSoloEntryDao tournamentSoloEntryDao,
            final TournamentTeamDao tournamentTeamDao,
            final TournamentMailService tournamentMailService,
            final MessageSource messageSource,
            final Clock clock) {
        this.tournamentDao = tournamentDao;
        this.tournamentSoloEntryDao = tournamentSoloEntryDao;
        this.tournamentTeamDao = tournamentTeamDao;
        this.tournamentMailService = tournamentMailService;
        this.messageSource = messageSource;
        this.clock = clock;
    }

    @Override
    @Transactional
    public TournamentSoloEntry joinSolo(final long tournamentId, final User user) {
        validateUser(user);
        final Tournament tournament = findTournamentOrThrow(tournamentId);
        requireRegistrationOpen(tournament);

        if (!tournament.isAllowSoloSignup()) {
            throw registrationException(
                    TournamentJoinFailureReason.SOLO_SIGNUP_DISABLED,
                    "tournament.registration.error.soloDisabled");
        }

        if (tournamentTeamDao.findUserTeam(tournamentId, user.getId()).isPresent()) {
            throw registrationException(
                    TournamentJoinFailureReason.ALREADY_ON_TEAM,
                    "tournament.registration.error.alreadyOnTeam");
        }

        final Optional<TournamentSoloEntry> existing =
                tournamentSoloEntryDao.findByTournamentAndUser(tournamentId, user.getId());
        if (existing.isEmpty()) {
            return tournamentSoloEntryDao.create(
                    tournament, user, TournamentSoloEntryStatus.IN_POOL);
        }

        final TournamentSoloEntry soloEntry = existing.get();
        if (TournamentSoloEntryStatus.IN_POOL == soloEntry.getStatus()) {
            return soloEntry;
        }
        if (TournamentSoloEntryStatus.ASSIGNED == soloEntry.getStatus()) {
            throw registrationException(
                    TournamentJoinFailureReason.ALREADY_ASSIGNED,
                    "tournament.registration.error.alreadyAssigned");
        }

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
                                        registrationException(
                                                TournamentJoinFailureReason.NOT_IN_SOLO_POOL,
                                                "tournament.registration.error.notInSoloPool"));

        soloEntry.setStatus(TournamentSoloEntryStatus.LEFT);
        soloEntry.setAssignedTeam(null);
        soloEntry.setLeftAt(Instant.now(clock));
        tournamentSoloEntryDao.update(soloEntry);
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
        return tournamentTeamDao.findUserTeam(tournamentId, user.getId());
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
                Math.toIntExact(tournamentTeamDao.countByTournament(tournamentId));
        final int availableTeamSlots = Math.max(0, tournament.getBracketSize() - existingTeamCount);
        final int soloTeamCount =
                Math.min(activeEntries.size() / tournament.getTeamSize(), availableTeamSlots);

        if (existingTeamCount + soloTeamCount < tournament.getBracketSize()) {
            markUnassigned(activeEntries);
            return cancelUnderCapacity(tournament);
        }

        final int assignableEntries = soloTeamCount * tournament.getTeamSize();
        final Set<String> usedTeamNames = existingTeamNames(tournamentId);
        for (int teamIndex = 0; teamIndex < soloTeamCount; teamIndex++) {
            final TournamentTeam team =
                    tournamentTeamDao.create(
                            tournament,
                            nextSoloTeamName(usedTeamNames),
                            TournamentTeamOrigin.SOLO_POOL,
                            null);

            final int firstEntryIndex = teamIndex * tournament.getTeamSize();
            for (int playerOffset = 0; playerOffset < tournament.getTeamSize(); playerOffset++) {
                final TournamentSoloEntry soloEntry =
                        activeEntries.get(firstEntryIndex + playerOffset);
                tournamentTeamDao.addMember(team, soloEntry.getUser(), false);
                soloEntry.setStatus(TournamentSoloEntryStatus.ASSIGNED);
                soloEntry.setAssignedTeam(team);
                tournamentSoloEntryDao.update(soloEntry);
            }
        }

        markUnassigned(activeEntries.subList(assignableEntries, activeEntries.size()));

        final Instant now = Instant.now(clock);
        tournament.setStatus(TournamentStatus.BRACKET_SETUP);
        tournament.setRegistrationClosedAt(now);
        tournament.setUpdatedAt(now);
        return tournamentDao.update(tournament);
    }

    private Tournament findTournamentOrThrow(final long tournamentId) {
        return tournamentDao
                .findById(tournamentId)
                .filter(tournament -> !tournament.isDeleted())
                .orElseThrow(
                        () ->
                                registrationException(
                                        TournamentJoinFailureReason.TOURNAMENT_NOT_FOUND,
                                        "tournament.registration.error.notFound"));
    }

    private void requireRegistrationOpen(final Tournament tournament) {
        if (TournamentStatus.REGISTRATION != tournament.getStatus()) {
            throw registrationException(
                    TournamentJoinFailureReason.REGISTRATION_NOT_OPEN,
                    "tournament.registration.error.notOpen");
        }
    }

    private void validateCanMutate(final Tournament tournament, final User actingUser) {
        if (!canMutate(tournament, actingUser)) {
            throw registrationException(
                    TournamentJoinFailureReason.FORBIDDEN,
                    "tournament.registration.error.forbidden");
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
            throw new IllegalArgumentException(message("user.error.null"));
        }
    }

    private Set<String> existingTeamNames(final long tournamentId) {
        final Set<String> names = new HashSet<>();
        for (final TournamentTeam team : tournamentTeamDao.findByTournament(tournamentId)) {
            names.add(team.getName());
        }
        return names;
    }

    private String nextSoloTeamName(final Set<String> usedTeamNames) {
        int index = 1;
        String name = soloTeamName(index);
        while (usedTeamNames.contains(name)) {
            index++;
            name = soloTeamName(index);
        }
        usedTeamNames.add(name);
        return name;
    }

    private void markUnassigned(final List<TournamentSoloEntry> soloEntries) {
        for (final TournamentSoloEntry soloEntry : soloEntries) {
            soloEntry.setStatus(TournamentSoloEntryStatus.UNASSIGNED);
            soloEntry.setAssignedTeam(null);
            tournamentSoloEntryDao.update(soloEntry);
        }
    }

    private Tournament cancelUnderCapacity(final Tournament tournament) {
        final Instant now = Instant.now(clock);
        tournament.setStatus(TournamentStatus.CANCELLED);
        tournament.setRegistrationClosedAt(now);
        tournament.setCancelledAt(now);
        tournament.setCancelReason(message("tournament.registration.close.underCapacity"));
        tournament.setUpdatedAt(now);
        final Tournament updatedTournament = tournamentDao.update(tournament);
        tournamentMailService.sendTournamentCancelledEmail(updatedTournament);
        return updatedTournament;
    }

    private String soloTeamName(final int index) {
        final Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(
                "tournament.team.solo.name",
                new Object[] {index},
                "Solo squad #" + index,
                Objects.requireNonNull(locale));
    }

    private TournamentRegistrationException registrationException(
            final TournamentJoinFailureReason reason, final String messageCode) {
        return new TournamentRegistrationException(reason, message(messageCode));
    }

    private String message(final String code) {
        final Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(
                Objects.requireNonNull(code), null, code, Objects.requireNonNull(locale));
    }
}
