package ar.edu.itba.paw.webapp.viewmodel;

import java.util.List;

public class TournamentBracketViewModel {

    private final Long tournamentId;
    private final String title;
    private final String statusLabel;
    private final String statusTone;
    private final boolean generated;
    private final boolean publishable;
    private final List<RoundViewModel> rounds;

    public TournamentBracketViewModel(
            final Long tournamentId,
            final String title,
            final String statusLabel,
            final String statusTone,
            final boolean generated,
            final boolean publishable,
            final List<RoundViewModel> rounds) {
        this.tournamentId = tournamentId;
        this.title = title;
        this.statusLabel = statusLabel;
        this.statusTone = statusTone;
        this.generated = generated;
        this.publishable = publishable;
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

    public boolean isPublishable() {
        return publishable;
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
        private final String startDate;
        private final String startTime;
        private final String endDate;
        private final String endTime;
        private final String address;
        private final String latitude;
        private final String longitude;

        public MatchViewModel(
                final Long id,
                final String label,
                final String teamA,
                final String teamB,
                final String statusLabel,
                final boolean focused,
                final String startDate,
                final String startTime,
                final String endDate,
                final String endTime,
                final String address,
                final String latitude,
                final String longitude) {
            this.id = id;
            this.label = label;
            this.teamA = teamA;
            this.teamB = teamB;
            this.statusLabel = statusLabel;
            this.focused = focused;
            this.startDate = startDate;
            this.startTime = startTime;
            this.endDate = endDate;
            this.endTime = endTime;
            this.address = address;
            this.latitude = latitude;
            this.longitude = longitude;
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

        public String getStartDate() {
            return startDate;
        }

        public String getStartTime() {
            return startTime;
        }

        public String getEndDate() {
            return endDate;
        }

        public String getEndTime() {
            return endTime;
        }

        public String getAddress() {
            return address;
        }

        public String getLatitude() {
            return latitude;
        }

        public String getLongitude() {
            return longitude;
        }
    }
}
