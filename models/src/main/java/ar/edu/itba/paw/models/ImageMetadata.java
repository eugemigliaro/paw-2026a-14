package ar.edu.itba.paw.models;

public class ImageMetadata {

    private final Long id;
    private final String contentType;
    private final long contentLength;

    public ImageMetadata(final Long id, final String contentType, final long contentLength) {
        this.id = id;
        this.contentType = contentType;
        this.contentLength = contentLength;
    }

    public Long getId() {
        return id;
    }

    public String getContentType() {
        return contentType;
    }

    public long getContentLength() {
        return contentLength;
    }
}
