package ar.edu.itba.paw.services.exceptions.match;

import ar.edu.itba.paw.services.exceptions.NotFoundException;

public class MatchNotFoundException extends NotFoundException {
    public MatchNotFoundException() {
        super("notFound");
    }
}
