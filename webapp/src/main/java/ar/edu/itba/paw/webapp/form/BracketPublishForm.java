package ar.edu.itba.paw.webapp.form;

import ar.edu.itba.paw.webapp.validation.ValidBracketPublishForm;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;

@ValidBracketPublishForm
public class BracketPublishForm {

    private ZoneId tz = ZoneId.systemDefault();

    @Valid private List<BracketPublishScheduleForm> schedules = new ArrayList<>();

    public ZoneId getTz() {
        return tz;
    }

    public void setTz(final ZoneId tz) {
        this.tz = tz;
    }

    public List<BracketPublishScheduleForm> getSchedules() {
        return schedules;
    }

    public void setSchedules(final List<BracketPublishScheduleForm> schedules) {
        this.schedules = schedules;
    }
}
