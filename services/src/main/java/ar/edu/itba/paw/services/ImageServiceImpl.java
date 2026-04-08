package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.ImageMetadata;
import ar.edu.itba.paw.persistence.ImageDao;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ImageServiceImpl implements ImageService {

    public static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024L * 1024L;

    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final ImageDao imageDao;

    @Autowired
    public ImageServiceImpl(final ImageDao imageDao) {
        this.imageDao = imageDao;
    }

    @Override
    public Long store(
            final String contentType, final long contentLength, final InputStream contentStream)
            throws IOException {
        validateContentType(contentType);
        validateContentLength(contentLength);
        return imageDao.create(normalizeContentType(contentType), contentLength, contentStream);
    }

    @Override
    public Optional<ImageMetadata> findMetadataById(final Long imageId) {
        return imageDao.findMetadataById(imageId);
    }

    @Override
    public boolean streamContentById(final Long imageId, final OutputStream outputStream)
            throws IOException {
        return imageDao.streamContentById(imageId, outputStream);
    }

    private static String normalizeContentType(final String contentType) {
        return contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
    }

    private static void validateContentType(final String contentType) {
        final String normalized = normalizeContentType(contentType);
        if (normalized.isBlank() || !ALLOWED_CONTENT_TYPES.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported image format.");
        }
    }

    private static void validateContentLength(final long contentLength) {
        if (contentLength <= 0) {
            throw new IllegalArgumentException("Image file is empty.");
        }
        if (contentLength > MAX_IMAGE_SIZE_BYTES) {
            throw new IllegalArgumentException("Image exceeds the 5 MB size limit.");
        }
    }
}
