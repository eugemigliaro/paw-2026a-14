package ar.edu.itba.paw.models.exceptions.tournament;

import ar.edu.itba.paw.models.exceptions.NotFoundException;

public class TournamentNotFoundException extends NotFoundException {
    public TournamentNotFoundException() {
        super("notFound");
    }
}
