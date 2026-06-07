package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.TournamentMatch;
import ar.edu.itba.paw.models.TournamentTeam;
import ar.edu.itba.paw.models.TournamentTeamMember;
import ar.edu.itba.paw.models.User;
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
    private final MailDispatchService mailDispatchService;

    public TournamentMailServiceImpl(
            final TournamentTeamDataService tournamentTeamDataService,
            final MailDispatchService mailDispatchService) {
        this.tournamentTeamDataService = tournamentTeamDataService;
        this.mailDispatchService = mailDispatchService;
    }

    @Override
    public void sendBracketPublishedEmail(final Tournament tournament) {
        for (final User recipient : recipients(tournament)) {
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
        for (final User recipient : recipients(tournament)) {
            mailDispatchService.sendTournamentCancelled(recipient, tournament);
        }
    }

    private List<User> recipients(final Tournament tournament) {
        if (tournament == null || tournament.getId() == null) {
            return List.of();
        }

        final Map<String, User> recipientsByIdentity = new LinkedHashMap<>();
        for (final TournamentTeamMember member :
                tournamentTeamDataService.findMembersByTournament(tournament.getId())) {
            final User user = member == null ? null : member.getUser();
            if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
                continue;
            }
            recipientsByIdentity.putIfAbsent(userIdentity(user), user);
        }
        return List.copyOf(recipientsByIdentity.values());
    }

    private static String userIdentity(final User user) {
        if (user.getId() != null) {
            return "id:" + user.getId();
        }
        return "email:" + user.getEmail().trim().toLowerCase(Locale.ROOT);
    }
}
