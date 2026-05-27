package ar.edu.itba.paw.webapp.viewmodel;

import java.util.List;

public class TournamentBracketViewModel {

    private final Long tournamentId;
    private final String title;
    private final String statusLabel;
    private final String statusTone;
    private final boolean generated;
    private final boolean publishable;
    private final boolean canManageResults;
    private final List<RoundViewModel> rounds;
    private final List<TeamRosterViewModel> teamRosters;

    public TournamentBracketViewModel(
            final Long tournamentId,
            final String title,
            final String statusLabel,
            final String statusTone,
            final boolean generated,
            final boolean publishable,
            final boolean canManageResults,
            final List<RoundViewModel> rounds) {
        this(
                tournamentId,
                title,
                statusLabel,
                statusTone,
                generated,
                publishable,
                canManageResults,
                rounds,
                List.of());
    }

    public TournamentBracketViewModel(
            final Long tournamentId,
            final String title,
            final String statusLabel,
            final String statusTone,
            final boolean generated,
            final boolean publishable,
            final boolean canManageResults,
            final List<RoundViewModel> rounds,
            final List<TeamRosterViewModel> teamRosters) {
        this.tournamentId = tournamentId;
        this.title = title;
        this.statusLabel = statusLabel;
        this.statusTone = statusTone;
        this.generated = generated;
        this.publishable = publishable;
        this.canManageResults = canManageResults;
        this.rounds = rounds == null ? List.of() : List.copyOf(rounds);
        this.teamRosters = teamRosters == null ? List.of() : List.copyOf(teamRosters);
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

    public boolean isCanManageResults() {
        return canManageResults;
    }

    public List<RoundViewModel> getRounds() {
        return rounds;
    }

    public List<TeamRosterViewModel> getTeamRosters() {
        return teamRosters;
    }

    public boolean isHasTeamRosters() {
        return !teamRosters.isEmpty();
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
        private final Long teamAId;
        private final Long teamBId;
        private final String label;
        private final String teamA;
        private final String teamB;
        private final String statusLabel;
        private final String statusTone;
        private final boolean teamAViewerTeam;
        private final boolean teamBViewerTeam;
        private final boolean teamAIsWinner;
        private final boolean teamBIsWinner;
        private final boolean isFinalRound;
        private final boolean canRecordResult;
        private final String scheduleLabel;
        private final String startDate;
        private final String startTime;
        private final String endDate;
        private final String endTime;
        private final String address;
        private final String latitude;
        private final String longitude;

        public MatchViewModel(
                final Long id,
                final Long teamAId,
                final Long teamBId,
                final String label,
                final String teamA,
                final String teamB,
                final String statusLabel,
                final String statusTone,
                final boolean teamAViewerTeam,
                final boolean teamBViewerTeam,
                final boolean teamAIsWinner,
                final boolean teamBIsWinner,
                final boolean isFinalRound,
                final boolean canRecordResult,
                final String scheduleLabel,
                final String startDate,
                final String startTime,
                final String endDate,
                final String endTime,
                final String address,
                final String latitude,
                final String longitude) {
            this.id = id;
            this.teamAId = teamAId;
            this.teamBId = teamBId;
            this.label = label;
            this.teamA = teamA;
            this.teamB = teamB;
            this.statusLabel = statusLabel;
            this.statusTone = statusTone;
            this.teamAViewerTeam = teamAViewerTeam;
            this.teamBViewerTeam = teamBViewerTeam;
            this.teamAIsWinner = teamAIsWinner;
            this.teamBIsWinner = teamBIsWinner;
            this.isFinalRound = isFinalRound;
            this.canRecordResult = canRecordResult;
            this.scheduleLabel = scheduleLabel;
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

        public Long getTeamAId() {
            return teamAId;
        }

        public Long getTeamBId() {
            return teamBId;
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

        public String getStatusTone() {
            return statusTone;
        }

        public boolean isTeamAViewerTeam() {
            return teamAViewerTeam;
        }

        public boolean isTeamBViewerTeam() {
            return teamBViewerTeam;
        }

        public boolean isTeamAIsWinner() {
            return teamAIsWinner;
        }

        public boolean isTeamBIsWinner() {
            return teamBIsWinner;
        }

        public boolean isFinalRound() {
            return isFinalRound;
        }

        public boolean isCanRecordResult() {
            return canRecordResult;
        }

        public String getScheduleLabel() {
            return scheduleLabel;
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

    public static class TeamRosterViewModel {

        private final String teamName;
        private final String membersLabel;

        public TeamRosterViewModel(final String teamName, final List<String> memberUsernames) {
            this.teamName = teamName;
            this.membersLabel =
                    String.join(", ", memberUsernames == null ? List.of() : memberUsernames);
        }

        public String getTeamName() {
            return teamName;
        }

        public String getMembersLabel() {
            return membersLabel;
        }

        public boolean isHasMembers() {
            return !membersLabel.isBlank();
        }
    }
}
