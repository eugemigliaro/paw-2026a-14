package ar.edu.itba.paw.services.exceptions.user;

import ar.edu.itba.paw.services.exceptions.NotFoundException;

public class UserNotFoundException extends NotFoundException {
    public UserNotFoundException() {
        super("exception.user.notFound");
    }
}
