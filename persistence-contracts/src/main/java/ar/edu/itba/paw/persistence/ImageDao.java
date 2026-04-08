package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.ImageMetadata;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

public interface ImageDao {

    Long create(String contentType, long contentLength, InputStream contentStream)
            throws IOException;

    Optional<ImageMetadata> findMetadataById(Long imageId);

    boolean streamContentById(Long imageId, OutputStream outputStream) throws IOException;
}
