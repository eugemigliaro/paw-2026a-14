package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class ModerationResolutionForm {

    public interface BanAction {}

    @Size(max = 4000, message = "{Size.moderationResolutionForm.resolutionDetails}")
    private String resolutionDetails;

    @NotNull(groups = BanAction.class, message = "{NotNull.moderationResolutionForm.banDays}")
    @Min(value = 1, groups = BanAction.class, message = "{Min.moderationResolutionForm.banDays}")
    @Max(value = 365, groups = BanAction.class, message = "{Max.moderationResolutionForm.banDays}")
    private Integer banDays = 7;

    public String getResolutionDetails() {
        return resolutionDetails;
    }

    public void setResolutionDetails(final String resolutionDetails) {
        this.resolutionDetails = resolutionDetails;
    }

    public Integer getBanDays() {
        return banDays;
    }

    public void setBanDays(final Integer banDays) {
        this.banDays = banDays;
    }
}
