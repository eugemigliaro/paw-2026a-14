package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.User;
import java.util.Optional;

public interface MvpIdentityService {

    Optional<User> findExistingByEmail(String email);

    User resolveOrCreateByEmail(String email);
}
