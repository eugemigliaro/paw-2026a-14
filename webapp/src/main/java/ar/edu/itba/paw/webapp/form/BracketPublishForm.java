package ar.edu.itba.paw.webapp.form;

import ar.edu.itba.paw.webapp.validation.ValidBracketPublishForm;
import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;

@ValidBracketPublishForm
public class BracketPublishForm {

    @Valid private List<BracketPublishScheduleForm> schedules = new ArrayList<>();

    public List<BracketPublishScheduleForm> getSchedules() {
        return schedules;
    }

    public void setSchedules(final List<BracketPublishScheduleForm> schedules) {
        this.schedules = schedules;
    }
}
