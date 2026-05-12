package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.Image;
import ar.edu.itba.paw.models.ImageMetadata;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class ImageJpaDao implements ImageDao {

    @PersistenceContext private EntityManager em;

    @Override
    @Transactional
    public Long create(
            final String contentType, final long contentLength, final InputStream contentStream)
            throws IOException {
        final byte[] content = contentStream.readAllBytes();

        final Image image = new Image(null, contentType, contentLength, content, Instant.now());

        em.persist(image);
        em.flush();

        final Long id = image.getId();
        if (id == null) {
            throw new IllegalStateException("Image insert did not return an id");
        }
        return id;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ImageMetadata> findMetadataById(final Long imageId) {
        return Optional.ofNullable(em.find(Image.class, imageId))
                .map(
                        image ->
                                new ImageMetadata(
                                        image.getId(),
                                        image.getContentType(),
                                        image.getContentLength()));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean streamContentById(final Long imageId, final OutputStream outputStream)
            throws IOException {
        final Image image = em.find(Image.class, imageId);

        if (image == null) {
            return false;
        }

        final byte[] content = image.getContent();
        if (content == null) {
            return false;
        }

        try {
            outputStream.write(content);
            outputStream.flush();
        } catch (final IOException exception) {
            throw exception;
        }

        return true;
    }
}
