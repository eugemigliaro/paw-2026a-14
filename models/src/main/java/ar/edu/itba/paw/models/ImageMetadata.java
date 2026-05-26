package ar.edu.itba.paw.models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name = "images")
public class ImageMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "images_imageid_seq")
    @SequenceGenerator(
            sequenceName = "images_imageid_seq",
            name = "images_imageid_seq",
            allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "content_type", length = 100, nullable = false)
    private String contentType;

    @Column(name = "content_length", nullable = false)
    private long contentLength;

    // Default no-arg constructor for JPA
    ImageMetadata() {}

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

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }

    @Override
    public String toString() {
        return "ImageMetadata{"
                + "id="
                + id
                + ", contentType='"
                + contentType
                + '\''
                + ", contentLength="
                + contentLength
                + '}';
    }
}
