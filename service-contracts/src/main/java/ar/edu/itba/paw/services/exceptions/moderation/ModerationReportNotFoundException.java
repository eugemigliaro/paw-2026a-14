package ar.edu.itba.paw.services.exceptions.moderation;

import ar.edu.itba.paw.services.exceptions.NotFoundException;

public class ModerationReportNotFoundException extends NotFoundException {
    public ModerationReportNotFoundException() {
        super("notFound");
    }
}
