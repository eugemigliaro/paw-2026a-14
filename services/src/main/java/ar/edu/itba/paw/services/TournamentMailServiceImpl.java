package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.TournamentMatch;
import ar.edu.itba.paw.models.TournamentSoloEntry;
import ar.edu.itba.paw.models.TournamentTeam;
import ar.edu.itba.paw.models.TournamentTeamMember;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.types.TournamentSoloEntryStatus;
import ar.edu.itba.paw.persistence.TournamentSoloEntryDao;
import ar.edu.itba.paw.services.internal.TournamentTeamDataService;
import ar.edu.itba.paw.services.mail.MailDispatchService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class TournamentMailServiceImpl implements TournamentMailService {

    private final TournamentTeamDataService tournamentTeamDataService;
    private final TournamentSoloEntryDao tournamentSoloEntryDao;
    private final MailDispatchService mailDispatchService;

    public TournamentMailServiceImpl(
            final TournamentTeamDataService tournamentTeamDataService,
            final TournamentSoloEntryDao tournamentSoloEntryDao,
            final MailDispatchService mailDispatchService) {
        this.tournamentTeamDataService = tournamentTeamDataService;
        this.tournamentSoloEntryDao = tournamentSoloEntryDao;
        this.mailDispatchService = mailDispatchService;
    }

    @Override
    public void sendBracketPublishedEmail(final Tournament tournament) {
        // Reaches placed players (team members) and, for now, unplaced (UNASSIGNED) players too.
        for (final User recipient : bracketPublishedRecipients(tournament)) {
            mailDispatchService.sendTournamentBracketPublished(recipient, tournament);
        }
    }

    @Override
    public void sendMatchResultEmail(
            final Tournament tournament,
            final TournamentMatch match,
            final TournamentTeam winner,
            final TournamentTeam loser) {
        for (final User recipient : recipients(tournament)) {
            mailDispatchService.sendTournamentMatchResult(
                    recipient, tournament, match, winner, loser);
        }
    }

    @Override
    public void sendTournamentCompletedEmail(
            final Tournament tournament, final TournamentTeam champion) {
        for (final User recipient : recipients(tournament)) {
            mailDispatchService.sendTournamentCompleted(recipient, tournament, champion);
        }
    }

    @Override
    public void sendTournamentCancelledEmail(final Tournament tournament) {
        // A cancellation can happen at any stage, so it must reach everyone still registered:
        // team members plus the whole solo queue (not just team members, unlike result/completed
        // emails that fire once every player is already in a team).
        for (final User recipient : cancelledRecipients(tournament)) {
            mailDispatchService.sendTournamentCancelled(recipient, tournament);
        }
    }

    private List<User> cancelledRecipients(final Tournament tournament) {
        if (tournament == null || tournament.getId() == null) {
            return List.of();
        }

        final Map<String, User> recipientsByIdentity = new LinkedHashMap<>();
        collectTeamMembers(tournament, recipientsByIdentity);
        for (final TournamentSoloEntry entry :
                tournamentSoloEntryDao.findRegisteredByTournament(tournament.getId())) {
            addRecipient(recipientsByIdentity, entry == null ? null : entry.getUser());
        }
        return List.copyOf(recipientsByIdentity.values());
    }

    private List<User> recipients(final Tournament tournament) {
        if (tournament == null || tournament.getId() == null) {
            return List.of();
        }

        final Map<String, User> recipientsByIdentity = new LinkedHashMap<>();
        collectTeamMembers(tournament, recipientsByIdentity);
        return List.copyOf(recipientsByIdentity.values());
    }

    private List<User> bracketPublishedRecipients(final Tournament tournament) {
        if (tournament == null || tournament.getId() == null) {
            return List.of();
        }

        final Map<String, User> recipientsByIdentity = new LinkedHashMap<>();
        collectTeamMembers(tournament, recipientsByIdentity);
        for (final TournamentSoloEntry entry :
                tournamentSoloEntryDao.findByTournamentAndStatus(
                        tournament.getId(), TournamentSoloEntryStatus.UNASSIGNED)) {
            addRecipient(recipientsByIdentity, entry == null ? null : entry.getUser());
        }
        return List.copyOf(recipientsByIdentity.values());
    }

    private void collectTeamMembers(
            final Tournament tournament, final Map<String, User> recipientsByIdentity) {
        for (final TournamentTeamMember member :
                tournamentTeamDataService.findMembersByTournament(tournament.getId())) {
            addRecipient(recipientsByIdentity, member == null ? null : member.getUser());
        }
    }

    private static void addRecipient(
            final Map<String, User> recipientsByIdentity, final User user) {
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            return;
        }
        recipientsByIdentity.putIfAbsent(userIdentity(user), user);
    }

    private static String userIdentity(final User user) {
        if (user.getId() != null) {
            return "id:" + user.getId();
        }
        return "email:" + user.getEmail().trim().toLowerCase(Locale.ROOT);
    }
}
