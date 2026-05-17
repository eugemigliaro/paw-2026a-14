package ar.edu.itba.paw.models;

import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name = "images")
public class Image {

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
    private Long contentLength;

    @Column(name = "content", columnDefinition = "BYTEA", nullable = false)
    private byte[] content;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // Default no-arg constructor for JPA
    Image() {}

    public Image(
            final Long id,
            final String contentType,
            final Long contentLength,
            final byte[] content,
            final Instant createdAt) {
        this.id = id;
        this.contentType = contentType;
        this.contentLength = contentLength;
        this.content = content;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getContentType() {
        return contentType;
    }

    public Long getContentLength() {
        return contentLength;
    }

    public byte[] getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
