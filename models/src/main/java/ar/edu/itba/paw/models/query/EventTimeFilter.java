package ar.edu.itba.paw.models.query;

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
