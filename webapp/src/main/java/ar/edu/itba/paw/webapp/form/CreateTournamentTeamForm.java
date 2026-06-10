package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class CreateTournamentTeamForm {
    @NotBlank(message = "{CreateTournamentTeamForm.name.NotBlank}")
    @Size(max = 150, message = "{CreateTournamentTeamForm.name.Size}")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }
}
