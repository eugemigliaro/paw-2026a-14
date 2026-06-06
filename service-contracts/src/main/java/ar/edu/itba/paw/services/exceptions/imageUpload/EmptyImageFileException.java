package ar.edu.itba.paw.services.exceptions.imageUpload;

public class EmptyImageFileException extends ImageUploadException {
    public EmptyImageFileException() {
        super("exception.imageUpload.empty");
    }
}
