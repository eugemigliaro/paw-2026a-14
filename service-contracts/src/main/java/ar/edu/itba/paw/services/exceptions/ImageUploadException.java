package ar.edu.itba.paw.services.exceptions;

public class ImageUploadException extends RuntimeException {

    public static final String UNSUPPORTED_FORMAT = "unsupported_format";
    public static final String EMPTY_FILE = "empty_file";
    public static final String TOO_LARGE = "too_large";

    private final String code;

    public ImageUploadException(final String code) {
        super(code);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
