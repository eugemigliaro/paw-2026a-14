package ar.edu.itba.paw.models.exceptions.user;

import ar.edu.itba.paw.models.exceptions.NotFoundException;

public class UserNotFoundException extends NotFoundException {
    public UserNotFoundException() {
        super("notFound");
    }
}
