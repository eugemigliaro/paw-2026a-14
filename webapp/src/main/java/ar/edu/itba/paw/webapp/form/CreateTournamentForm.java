package ar.edu.itba.paw.webapp.form;

import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.webapp.validation.ValidCreateTournamentForm;
import ar.edu.itba.paw.webapp.validation.ValidImage;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

@ValidCreateTournamentForm
public class CreateTournamentForm {

    public CreateTournamentForm() {
        final LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        registrationOpensDate = now.toLocalDate();
        registrationOpensTime = now.toLocalTime();
        startDate = registrationClosesDate.plusDays(1);
        endDate = startDate;
    }

    @NotBlank(message = "{CreateTournamentForm.title.NotBlank}")
    @Size(max = 150, message = "{CreateTournamentForm.title.Size}")
    private String title = "";

    @Size(max = 4000, message = "{CreateTournamentForm.description.Size}")
    private String description = "";

    @NotBlank(message = "{CreateTournamentForm.address.NotBlank}")
    @Size(max = 255, message = "{CreateTournamentForm.address.Size}")
    private String address = "";

    private Double latitude;

    private Double longitude;

    @NotNull(message = "{CreateTournamentForm.sport.NotNull}")
    private Sport sport = Sport.PADEL;

    @NotNull(message = "{CreateTournamentForm.registrationOpensDate.NotNull}")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate registrationOpensDate;

    @NotNull(message = "{CreateTournamentForm.registrationOpensTime.NotNull}")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime registrationOpensTime;

    @NotNull(message = "{CreateTournamentForm.registrationClosesDate.NotNull}")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate registrationClosesDate = LocalDate.now().plusDays(10);

    @NotNull(message = "{CreateTournamentForm.registrationClosesTime.NotNull}")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime registrationClosesTime = LocalTime.of(20, 0);

    @NotNull(message = "{CreateTournamentForm.startDate.NotNull}")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @NotNull(message = "{CreateTournamentForm.startTime.NotNull}")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime startTime = LocalTime.of(18, 0);

    @NotNull(message = "{CreateTournamentForm.endDate.NotNull}")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate = startDate;

    @NotNull(message = "{CreateTournamentForm.endTime.NotNull}")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime endTime = LocalTime.of(21, 0);

    @NotNull(message = "{CreateTournamentForm.bracketSize.NotNull}")
    private Integer bracketSize = 8;

    @NotNull(message = "{CreateTournamentForm.teamSize.NotNull}")
    @Min(value = 1, message = "{CreateTournamentForm.teamSize.Min}")
    @Max(value = 100, message = "{CreateTournamentForm.teamSize.Max}")
    private Integer teamSize = 2;

    @NotNull(message = "{CreateTournamentForm.pricePerPlayer.NotNull}")
    @DecimalMin(
            value = "0",
            inclusive = true,
            message = "{CreateTournamentForm.pricePerPlayer.DecimalMin}")
    private BigDecimal pricePerPlayer = BigDecimal.ZERO;

    private boolean allowSoloSignup = true;

    private boolean allowTeamDraft = true;

    @ValidImage private MultipartFile bannerImage;

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(final String address) {
        this.address = address;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(final Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(final Double longitude) {
        this.longitude = longitude;
    }

    public Sport getSport() {
        return sport;
    }

    public void setSport(final Sport sport) {
        this.sport = sport;
    }

    public LocalDate getRegistrationOpensDate() {
        return registrationOpensDate;
    }

    public void setRegistrationOpensDate(final LocalDate registrationOpensDate) {
        this.registrationOpensDate = registrationOpensDate;
    }

    public LocalTime getRegistrationOpensTime() {
        return registrationOpensTime;
    }

    public void setRegistrationOpensTime(final LocalTime registrationOpensTime) {
        this.registrationOpensTime = registrationOpensTime;
    }

    public LocalDate getRegistrationClosesDate() {
        return registrationClosesDate;
    }

    public void setRegistrationClosesDate(final LocalDate registrationClosesDate) {
        this.registrationClosesDate = registrationClosesDate;
    }

    public LocalTime getRegistrationClosesTime() {
        return registrationClosesTime;
    }

    public void setRegistrationClosesTime(final LocalTime registrationClosesTime) {
        this.registrationClosesTime = registrationClosesTime;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(final LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(final LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(final LocalDate endDate) {
        this.endDate = endDate;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(final LocalTime endTime) {
        this.endTime = endTime;
    }

    public Integer getBracketSize() {
        return bracketSize;
    }

    public void setBracketSize(final Integer bracketSize) {
        this.bracketSize = bracketSize;
    }

    public Integer getTeamSize() {
        return teamSize;
    }

    public void setTeamSize(final Integer teamSize) {
        this.teamSize = teamSize;
    }

    public BigDecimal getPricePerPlayer() {
        return pricePerPlayer;
    }

    public void setPricePerPlayer(final BigDecimal pricePerPlayer) {
        this.pricePerPlayer = pricePerPlayer;
    }

    public boolean isAllowSoloSignup() {
        return allowSoloSignup;
    }

    public void setAllowSoloSignup(final boolean allowSoloSignup) {
        this.allowSoloSignup = allowSoloSignup;
    }

    public boolean isAllowTeamDraft() {
        return allowTeamDraft;
    }

    public void setAllowTeamDraft(final boolean allowTeamDraft) {
        this.allowTeamDraft = allowTeamDraft;
    }

    public MultipartFile getBannerImage() {
        return bannerImage;
    }

    public void setBannerImage(final MultipartFile bannerImage) {
        this.bannerImage = bannerImage;
    }
}
