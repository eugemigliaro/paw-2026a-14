package ar.edu.itba.paw.webapp.utils;

import ar.edu.itba.paw.models.Match;

public final class ImageUrlHelper {

    private ImageUrlHelper() {}

    public static String bannerUrlFor(final Match match) {
        return match.getBannerImageId() == null ? null : "/images/" + match.getBannerImageId();
    }
}
