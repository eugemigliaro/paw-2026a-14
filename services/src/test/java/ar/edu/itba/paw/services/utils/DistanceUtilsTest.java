package ar.edu.itba.paw.services.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DistanceUtilsTest {
    @Test
    public void testCalculateDistance() {
        // Test case 1: Distance between two points
        Double lat1 = 40.7128; // New York City
        Double lon1 = -74.0060;
        Double lat2 = 34.0522; // Los Angeles
        Double lon2 = -118.2437;
        Double expectedDistance = 3935.75; // Approximate distance in kilometers

        Double actualDistance = DistanceUtils.distanceKm(lat1, lon1, lat2, lon2);
        Assertions.assertEquals(expectedDistance, actualDistance, 0.01);
    }

    @Test
    public void testCalculateDistanceSamePoint() {
        // Test case 2: Distance between the same point should be zero
        Double lat = 40.7128; // New York City
        Double lon = -74.0060;
        Double expectedDistance = 0.0;

        Double actualDistance = DistanceUtils.distanceKm(lat, lon, lat, lon);
        Assertions.assertEquals(expectedDistance, actualDistance, 0.01);
    }
}
