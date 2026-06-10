package ar.edu.itba.paw.services.utils;

public final class DistanceUtils {
    private static final double EARTH_RADIUS_KM = 6371.0088;

    private DistanceUtils() {}

    public static double distanceKm(
            final double fromLatitude,
            final double fromLongitude,
            final double toLatitude,
            final double toLongitude) {
        final double fromLatRad = Math.toRadians(fromLatitude);
        final double toLatRad = Math.toRadians(toLatitude);
        final double deltaLatRad = Math.toRadians(toLatitude - fromLatitude);
        final double deltaLonRad = Math.toRadians(toLongitude - fromLongitude);
        final double a =
                Math.sin(deltaLatRad / 2) * Math.sin(deltaLatRad / 2)
                        + Math.cos(fromLatRad)
                                * Math.cos(toLatRad)
                                * Math.sin(deltaLonRad / 2)
                                * Math.sin(deltaLonRad / 2);
        final double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}
