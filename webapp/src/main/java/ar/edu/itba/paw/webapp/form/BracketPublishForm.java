package ar.edu.itba.paw.webapp.form;

import ar.edu.itba.paw.webapp.validation.ValidBracketPublishForm;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;

@ValidBracketPublishForm
public class BracketPublishForm {

    @Valid private List<BracketPublishScheduleForm> schedules = new ArrayList<>();

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant tournamentStart;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant tournamentEnd;

    public List<BracketPublishScheduleForm> getSchedules() {
        return schedules;
    }

    public void setSchedules(final List<BracketPublishScheduleForm> schedules) {
        this.schedules = schedules;
    }

    public Instant getTournamentStart() {
        return tournamentStart;
    }

    public void setTournamentStart(final Instant tournamentStart) {
        this.tournamentStart = tournamentStart;
    }

    public Instant getTournamentEnd() {
        return tournamentEnd;
    }

    public void setTournamentEnd(final Instant tournamentEnd) {
        this.tournamentEnd = tournamentEnd;
    }
}
