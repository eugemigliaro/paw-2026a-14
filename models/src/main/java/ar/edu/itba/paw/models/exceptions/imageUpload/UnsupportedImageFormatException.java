package ar.edu.itba.paw.models.exceptions.imageUpload;

public class UnsupportedImageFormatException extends ImageUploadException {
    public UnsupportedImageFormatException() {
        super("unsupportedFormat");
    }
}
