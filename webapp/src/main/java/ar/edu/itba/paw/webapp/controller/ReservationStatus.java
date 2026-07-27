package ar.edu.itba.paw.webapp.controller;

public enum ReservationStatus {
    CONFIRMED("confirmed"),
    CANCELLED("cancelled"),
    RECURRING_CONFIRMED("recurringConfirmed"),
    RECURRING_CANCELLED("recurringCancelled"),
    SERIES_CONFIRMED("seriesConfirmed"),
    SERIES_CANCELLED("seriesCancelled");

    private final String code;

    ReservationStatus(final String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
