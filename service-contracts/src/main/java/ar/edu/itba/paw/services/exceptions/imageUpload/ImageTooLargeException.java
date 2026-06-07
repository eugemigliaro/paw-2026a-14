package ar.edu.itba.paw.services.exceptions.imageUpload;

public class ImageTooLargeException extends ImageUploadException {
    public ImageTooLargeException() {
        super("tooLarge");
    }
}
