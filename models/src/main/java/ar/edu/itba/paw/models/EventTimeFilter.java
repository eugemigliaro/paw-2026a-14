package ar.edu.itba.paw.models;

public enum EventTimeFilter {
    ALL,
    FUTURE,
    TODAY,
    TOMORROW,
    WEEK;

    @Override
    public String toString() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
