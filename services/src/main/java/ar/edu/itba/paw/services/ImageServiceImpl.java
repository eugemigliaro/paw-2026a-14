package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.ImageMetadata;
import ar.edu.itba.paw.models.exceptions.imageUpload.EmptyImageFileException;
import ar.edu.itba.paw.models.exceptions.imageUpload.ImageTooLargeException;
import ar.edu.itba.paw.models.exceptions.imageUpload.ImageUploadException;
import ar.edu.itba.paw.models.exceptions.imageUpload.UnsupportedImageFormatException;
import ar.edu.itba.paw.persistence.ImageDao;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ImageServiceImpl implements ImageService {

    public static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024L * 1024L;

    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("jpg", "jpeg", "png", "webp", "gif");

    private final ImageDao imageDao;

    @Autowired
    public ImageServiceImpl(final ImageDao imageDao) {
        this.imageDao = imageDao;
    }

    @Override
    @Transactional
    public Long store(
            final String contentType, final long contentLength, final InputStream contentStream)
            throws IOException {
        return storeAndReturnMetadata(contentType, contentLength, contentStream).getId();
    }

    private ImageMetadata storeAndReturnMetadata(
            final String contentType, final long contentLength, final InputStream contentStream)
            throws IOException {
        final String normalizedContentType = normalizeContentType(contentType);
        validateContentType(normalizedContentType);
        validateContentLength(contentLength);
        final byte[] content = readBounded(contentStream);
        validateContentLength(content.length);
        validateContentMatchesType(normalizedContentType, content);
        final Long imageId =
                imageDao.create(
                        normalizedContentType, content.length, new ByteArrayInputStream(content));
        return new ImageMetadata(imageId, normalizedContentType, content.length);
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

    @Override
    public ImageMetadata resolveImageMetadata(final ImageUpload image) {
        if (image == null || image.getContentLength() <= 0) {
            return null;
        }
        validateFilenameExtension(image.getOriginalFilename());
        try {
            return storeAndReturnMetadata(
                    image.getContentType(), image.getContentLength(), image.getContentStream());
        } catch (final IOException exception) {
            throw new ImageUploadException("exception.imageUpload.unavailable");
        }
    }

    private static String normalizeContentType(final String contentType) {
        if (contentType == null) {
            return "";
        }
        final String normalized = contentType.trim().toLowerCase(Locale.ROOT);
        if ("image/jpg".equals(normalized)) {
            return "image/jpeg";
        }
        return normalized;
    }

    private static void validateContentType(final String contentType) {
        final String normalized = normalizeContentType(contentType);
        if (normalized.isBlank() || !ALLOWED_CONTENT_TYPES.contains(normalized)) {
            throw new UnsupportedImageFormatException();
        }
    }

    private static void validateContentLength(final long contentLength) {
        if (contentLength <= 0) {
            throw new EmptyImageFileException();
        }
        if (contentLength > MAX_IMAGE_SIZE_BYTES) {
            throw new ImageTooLargeException();
        }
    }

    private static void validateFilenameExtension(final String filename) {
        if (filename == null || filename.isBlank()) {
            return;
        }
        final int extensionSeparator = filename.lastIndexOf('.');
        if (extensionSeparator < 0) {
            return;
        }
        if (extensionSeparator == filename.length() - 1) {
            throw new UnsupportedImageFormatException();
        }
        final String extension =
                filename.substring(extensionSeparator + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new UnsupportedImageFormatException();
        }
    }

    private static byte[] readBounded(final InputStream contentStream) throws IOException {
        if (contentStream == null) {
            return new byte[0];
        }
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        final byte[] buffer = new byte[8192];
        long totalBytes = 0;
        int readBytes;
        while ((readBytes = contentStream.read(buffer)) != -1) {
            totalBytes += readBytes;
            if (totalBytes > MAX_IMAGE_SIZE_BYTES) {
                throw new ImageTooLargeException();
            }
            output.write(buffer, 0, readBytes);
        }
        return output.toByteArray();
    }

    private static void validateContentMatchesType(
            final String normalizedContentType, final byte[] content) {
        if (content == null || content.length == 0) {
            throw new UnsupportedImageFormatException();
        }
        if (normalizedContentType == null || normalizedContentType.isBlank()) {
            throw new UnsupportedImageFormatException();
        }
        if (!ALLOWED_CONTENT_TYPES.contains(normalizedContentType)) {
            throw new UnsupportedImageFormatException();
        }

        final String detectedContentType = detectContentType(content);
        if (detectedContentType.isBlank() || !normalizedContentType.equals(detectedContentType)) {
            throw new UnsupportedImageFormatException();
        }
    }

    private static String detectContentType(final byte[] content) {
        if (content == null || content.length == 0) {
            throw new UnsupportedImageFormatException();
        }
        if (hasPrefix(content, new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF})) {
            return "image/jpeg";
        }
        if (hasPrefix(
                content, new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A})) {
            return "image/png";
        }
        if (hasPrefix(content, new byte[] {0x47, 0x49, 0x46, 0x38, 0x37, 0x61})
                || hasPrefix(content, new byte[] {0x47, 0x49, 0x46, 0x38, 0x39, 0x61})) {
            return "image/gif";
        }
        if (isWebp(content)) {
            return "image/webp";
        }
        return "";
    }

    private static boolean isWebp(final byte[] content) {
        return content != null
                && content.length >= 12
                && content[0] == 0x52
                && content[1] == 0x49
                && content[2] == 0x46
                && content[3] == 0x46
                && content[8] == 0x57
                && content[9] == 0x45
                && content[10] == 0x42
                && content[11] == 0x50;
    }

    private static boolean hasPrefix(final byte[] content, final byte[] prefix) {
        if (content == null || prefix == null || content.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (content[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}
