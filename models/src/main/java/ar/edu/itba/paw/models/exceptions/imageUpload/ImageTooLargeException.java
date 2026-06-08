package ar.edu.itba.paw.models.exceptions.imageUpload;

public class ImageTooLargeException extends ImageUploadException {
    public ImageTooLargeException() {
        super("tooLarge");
    }
}
