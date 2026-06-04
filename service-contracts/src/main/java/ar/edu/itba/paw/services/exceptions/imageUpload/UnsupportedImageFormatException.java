package ar.edu.itba.paw.services.exceptions.imageUpload;

public class UnsupportedImageFormatException extends ImageUploadException {
    public UnsupportedImageFormatException(final String message) {
        super(message);
    }
}
