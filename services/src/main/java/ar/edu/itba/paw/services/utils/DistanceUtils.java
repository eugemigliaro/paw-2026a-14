package ar.edu.itba.paw.services.utils;

public class DistanceUtils {
    private static final Double EARTH_RADIUS_KM = 6371.0088;

    private DistanceUtils() {}

    public static Double distanceKm(
            final Double fromLatitude,
            final Double fromLongitude,
            final Double toLatitude,
            final Double toLongitude) {
        final Double fromLatRad = Math.toRadians(fromLatitude);
        final Double toLatRad = Math.toRadians(toLatitude);
        final Double deltaLatRad = Math.toRadians(toLatitude - fromLatitude);
        final Double deltaLonRad = Math.toRadians(toLongitude - fromLongitude);
        final Double a =
                Math.sin(deltaLatRad / 2) * Math.sin(deltaLatRad / 2)
                        + Math.cos(fromLatRad)
                                * Math.cos(toLatRad)
                                * Math.sin(deltaLonRad / 2)
                                * Math.sin(deltaLonRad / 2);
        final Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}
