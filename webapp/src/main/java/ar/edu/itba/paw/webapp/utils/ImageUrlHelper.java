package ar.edu.itba.paw.webapp.utils;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.User;

public final class ImageUrlHelper {

    public static final String DEFAULT_PROFILE_IMAGE_URL = "/assets/default-profile-avatar.svg";

    private ImageUrlHelper() {}

    public static String bannerUrlFor(final Match match) {
        return match.hasBannerImage() ? "/images/" + match.getBannerImageMetadata().getId() : null;
    }

    public static String profileUrlFor(final User user) {
        return user.getProfileImageMetadata() == null
                ? DEFAULT_PROFILE_IMAGE_URL
                : "/images/" + user.getProfileImageMetadata().getId();
    }
}
