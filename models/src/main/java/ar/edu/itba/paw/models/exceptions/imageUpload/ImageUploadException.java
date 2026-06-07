package ar.edu.itba.paw.models.exceptions.imageUpload;

import ar.edu.itba.paw.models.exceptions.DomainException;

public class ImageUploadException extends DomainException {

    public ImageUploadException(final String message) {
        super(message);
    }
}
