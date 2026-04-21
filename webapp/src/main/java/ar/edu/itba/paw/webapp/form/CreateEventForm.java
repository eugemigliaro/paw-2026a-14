package ar.edu.itba.paw.webapp.form;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.FutureOrPresent;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

public class CreateEventForm {

    @NotBlank(message = "{CreateEventForm.title.NotBlank}")
    @Size(max = 150, message = "{CreateEventForm.title.Size}")
    private String title = "";

    @Size(max = 4000, message = "{CreateEventForm.description.Size}")
    private String description = "";

    @NotBlank(message = "{CreateEventForm.address.NotBlank}")
    @Size(max = 255, message = "{CreateEventForm.address.Size}")
    private String address = "";

    @NotBlank(message = "{CreateEventForm.sport.NotBlank}")
    private String sport = "padel";

    @NotNull(message = "{CreateEventForm.eventDate.NotNull}")
    @FutureOrPresent(message = "{CreateEventForm.eventDate.FutureOrPresent}")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate eventDate = LocalDate.now().plusDays(1);

    @NotNull(message = "{CreateEventForm.eventTime.NotNull}")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime eventTime = LocalTime.of(18, 0);

    @NotNull(message = "{CreateEventForm.endDate.NotNull}")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate = LocalDate.now().plusDays(1);

    @NotNull(message = "{CreateEventForm.endTime.NotNull}")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime endTime = LocalTime.of(19, 30);

    @NotNull(message = "{CreateEventForm.maxPlayers.NotNull}")
    @Min(value = 1, message = "{CreateEventForm.maxPlayers.Min}")
    private Integer maxPlayers = 8;

    @NotNull(message = "{CreateEventForm.pricePerPlayer.NotNull}")
    @DecimalMin(
            value = "0",
            inclusive = true,
            message = "{CreateEventForm.pricePerPlayer.DecimalMin}")
    private BigDecimal pricePerPlayer = BigDecimal.ZERO;

    private String tz = "";

    private MultipartFile bannerImage;

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

    public String getSport() {
        return sport;
    }

    public void setSport(final String sport) {
        this.sport = sport;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public void setEventDate(final LocalDate eventDate) {
        this.eventDate = eventDate;
    }

    public LocalTime getEventTime() {
        return eventTime;
    }

    public void setEventTime(final LocalTime eventTime) {
        this.eventTime = eventTime;
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

    public Integer getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(final Integer maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public BigDecimal getPricePerPlayer() {
        return pricePerPlayer;
    }

    public void setPricePerPlayer(final BigDecimal pricePerPlayer) {
        this.pricePerPlayer = pricePerPlayer;
    }

    public String getTz() {
        return tz;
    }

    public void setTz(final String tz) {
        this.tz = tz;
    }

    public MultipartFile getBannerImage() {
        return bannerImage;
    }

    public void setBannerImage(final MultipartFile bannerImage) {
        this.bannerImage = bannerImage;
    }
}
