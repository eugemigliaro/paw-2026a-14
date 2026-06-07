package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class ReportAppealForm {
    @Size(max = 4000, message = "{Size.reportForm.details}")
    @Pattern(
            regexp = "^[\\p{L}\\p{N} ,.;:()\"'\\-\\/ .!?\\n@&_*+=\\[\\]{}\\$#%^~|<>\\\\]*$",
            message = "{Pattern.reportForm.details}")
    private String details;

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
