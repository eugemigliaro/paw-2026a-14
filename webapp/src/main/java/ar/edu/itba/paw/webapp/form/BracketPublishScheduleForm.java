package ar.edu.itba.paw.webapp.form;

import java.time.LocalDate;
import java.time.LocalTime;
import org.springframework.format.annotation.DateTimeFormat;

public class BracketPublishScheduleForm {

    private Long matchId;

    private Integer roundNumber;

    private String roundLabel = "";

    private String matchLabel = "";

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime endTime;

    private String address = "";

    private Double latitude;

    private Double longitude;

    public Long getMatchId() {
        return matchId;
    }

    public void setMatchId(final Long matchId) {
        this.matchId = matchId;
    }

    public Integer getRoundNumber() {
        return roundNumber;
    }

    public void setRoundNumber(final Integer roundNumber) {
        this.roundNumber = roundNumber;
    }

    public String getRoundLabel() {
        return roundLabel;
    }

    public void setRoundLabel(final String roundLabel) {
        this.roundLabel = roundLabel;
    }

    public String getMatchLabel() {
        return matchLabel;
    }

    public void setMatchLabel(final String matchLabel) {
        this.matchLabel = matchLabel;
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
}
