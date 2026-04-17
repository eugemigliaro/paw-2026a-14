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

    @NotBlank(message = "Event title is required")
    @Size(max = 150, message = "Event title cannot exceed 150 characters")
    private String title = "";

    @Size(max = 4000, message = "Description cannot exceed 4000 characters")
    private String description = "";

    @NotBlank(message = "Location is required")
    @Size(max = 255, message = "Location cannot exceed 255 characters")
    private String address = "";

    @NotBlank(message = "Sport is required")
    private String sport = "padel";

    @NotNull(message = "Match date is required")
    @FutureOrPresent(message = "Match date cannot be in the past")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate eventDate = LocalDate.now().plusDays(1);

    @NotNull(message = "Event time is required")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime eventTime = LocalTime.of(18, 0);

    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime endTime;

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer maxPlayers = 8;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0", inclusive = true, message = "Price cannot be negative")
    private BigDecimal pricePerPlayer = BigDecimal.ZERO;

    private String timezone = "";

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

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(final String timezone) {
        this.timezone = timezone;
    }

    public MultipartFile getBannerImage() {
        return bannerImage;
    }

    public void setBannerImage(final MultipartFile bannerImage) {
        this.bannerImage = bannerImage;
    }
}
