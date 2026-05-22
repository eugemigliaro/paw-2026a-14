package ar.edu.itba.paw.webapp.viewmodel;

import java.util.List;

public class TournamentBracketViewModel {

    private final Long tournamentId;
    private final String title;
    private final String statusLabel;
    private final String statusTone;
    private final boolean generated;
    private final List<RoundViewModel> rounds;

    public TournamentBracketViewModel(
            final Long tournamentId,
            final String title,
            final String statusLabel,
            final String statusTone,
            final boolean generated,
            final List<RoundViewModel> rounds) {
        this.tournamentId = tournamentId;
        this.title = title;
        this.statusLabel = statusLabel;
        this.statusTone = statusTone;
        this.generated = generated;
        this.rounds = rounds == null ? List.of() : List.copyOf(rounds);
    }

    public Long getTournamentId() {
        return tournamentId;
    }

    public String getTitle() {
        return title;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public String getStatusTone() {
        return statusTone;
    }

    public boolean isGenerated() {
        return generated;
    }

    public List<RoundViewModel> getRounds() {
        return rounds;
    }

    public static class RoundViewModel {

        private final int roundNumber;
        private final String label;
        private final List<MatchViewModel> matches;

        public RoundViewModel(
                final int roundNumber, final String label, final List<MatchViewModel> matches) {
            this.roundNumber = roundNumber;
            this.label = label;
            this.matches = matches == null ? List.of() : List.copyOf(matches);
        }

        public int getRoundNumber() {
            return roundNumber;
        }

        public String getLabel() {
            return label;
        }

        public List<MatchViewModel> getMatches() {
            return matches;
        }
    }

    public static class MatchViewModel {

        private final Long id;
        private final String label;
        private final String teamA;
        private final String teamB;
        private final String statusLabel;
        private final boolean focused;

        public MatchViewModel(
                final Long id,
                final String label,
                final String teamA,
                final String teamB,
                final String statusLabel,
                final boolean focused) {
            this.id = id;
            this.label = label;
            this.teamA = teamA;
            this.teamB = teamB;
            this.statusLabel = statusLabel;
            this.focused = focused;
        }

        public Long getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }

        public String getTeamA() {
            return teamA;
        }

        public String getTeamB() {
            return teamB;
        }

        public String getStatusLabel() {
            return statusLabel;
        }

        public boolean isFocused() {
            return focused;
        }
    }
}
