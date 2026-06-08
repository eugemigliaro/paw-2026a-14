package ar.edu.itba.paw.models.exceptions.moderation;

import ar.edu.itba.paw.models.exceptions.NotFoundException;

public class ModerationReportNotFoundException extends NotFoundException {
    public ModerationReportNotFoundException() {
        super("notFound");
    }
}
