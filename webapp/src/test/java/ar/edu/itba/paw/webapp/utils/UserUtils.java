package ar.edu.itba.paw.webapp.utils;

import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserLanguages;

public class UserUtils {
    public static User getUser(Long id) {
        return new User(
                id,
                "user" + id + "@test.com",
                "user" + id,
                "Name" + id,
                "LastName" + id,
                null,
                null,
                UserLanguages.DEFAULT_LANGUAGE);
    }
}
