package ar.edu.itba.paw.models.exceptions.imageUpload;

public class EmptyImageFileException extends ImageUploadException {
    public EmptyImageFileException() {
        super("empty");
    }
}
