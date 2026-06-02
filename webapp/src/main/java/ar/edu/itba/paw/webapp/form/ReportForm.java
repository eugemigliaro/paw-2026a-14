package ar.edu.itba.paw.webapp.form;

import ar.edu.itba.paw.models.types.ReportReason;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class ReportForm {

    @NotNull(message = "{NotBlank.reportForm.reason}")
    private ReportReason reason;

    @Size(max = 4000, message = "{Size.reportForm.details}")
    @Pattern(
            regexp = "^[\\p{L}\\p{N} ,.;:()\"'\\-\\/ .!?\\n@&_*+=\\[\\]{}\\$#%^~|<>\\\\]*$",
            message = "{Pattern.reportForm.details}")
    private String details;

    public ReportReason getReason() {
        return reason;
    }

    public void setReason(ReportReason reason) {
        this.reason = reason;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
