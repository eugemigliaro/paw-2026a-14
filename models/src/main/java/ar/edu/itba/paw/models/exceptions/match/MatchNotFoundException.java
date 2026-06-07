package ar.edu.itba.paw.models.exceptions.match;

import ar.edu.itba.paw.models.exceptions.NotFoundException;

public class MatchNotFoundException extends NotFoundException {
    public MatchNotFoundException() {
        super("notFound");
    }
}
