package ar.edu.itba.paw.services.exceptions.tournament;

import ar.edu.itba.paw.services.exceptions.NotFoundException;

public class TournamentNotFoundException extends NotFoundException {
    public TournamentNotFoundException() {
        super("notFound");
    }
}
