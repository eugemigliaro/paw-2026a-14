package ar.edu.itba.paw.services.exceptions.imageUpload;

import ar.edu.itba.paw.services.exceptions.DomainException;

public class ImageUploadException extends DomainException {

    public ImageUploadException(final String message) {
        super(message);
    }
}
