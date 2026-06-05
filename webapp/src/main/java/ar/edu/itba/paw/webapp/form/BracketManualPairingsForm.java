package ar.edu.itba.paw.webapp.form;

import ar.edu.itba.paw.webapp.validation.ValidBracketManualPairingsForm;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.Min;

@ValidBracketManualPairingsForm
public class BracketManualPairingsForm {

    private List<Long> teamIds = new ArrayList<>();

    @Min(value = 1, message = "{tournament.bracket.manualPairings.validation.teamCount}")
    private Integer expectedTeamCount = 1;

    public List<Long> getTeamIds() {
        return teamIds;
    }

    public void setTeamIds(final List<Long> teamIds) {
        this.teamIds = teamIds;
    }

    public Integer getExpectedTeamCount() {
        return expectedTeamCount;
    }

    public void setExpectedTeamCount(final Integer expectedTeamCount) {
        this.expectedTeamCount = expectedTeamCount;
    }
}
