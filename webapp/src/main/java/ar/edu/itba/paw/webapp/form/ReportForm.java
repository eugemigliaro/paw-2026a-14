package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class ReportForm {

    @NotBlank(message = "{NotBlank.reportForm.reason}")
    @Pattern(
            regexp = "^(inappropriate_content|aggressive_language|spam|harassment|cheating|other)$",
            message = "{Pattern.reportForm.reason}")
    private String reason;

    @Size(max = 4000, message = "{Size.reportForm.details}")
    @Pattern(
            regexp = "^[\\p{L}\\p{N} ,.;:()\"'\\-\\/ .!?\\n]*$",
            message = "{Pattern.reportForm.details}")
    private String details;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
