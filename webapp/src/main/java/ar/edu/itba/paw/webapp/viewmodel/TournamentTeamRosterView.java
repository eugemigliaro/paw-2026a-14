package ar.edu.itba.paw.webapp.viewmodel;

import ar.edu.itba.paw.models.TournamentTeam;
import ar.edu.itba.paw.models.TournamentTeamMember;
import java.util.List;

public final class TournamentTeamRosterView {
    private final TournamentTeam team;
    private final List<TournamentTeamMember> members;
    private final int memberCount;
    private final boolean full;

    public TournamentTeamRosterView(
            final TournamentTeam team,
            final List<TournamentTeamMember> members,
            final int teamSize) {
        this.team = team;
        this.members = members;
        this.memberCount = members.size();
        this.full = members.size() >= teamSize;
    }

    public TournamentTeam getTeam() {
        return team;
    }

    public List<TournamentTeamMember> getMembers() {
        return members;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public boolean isFull() {
        return full;
    }
}
