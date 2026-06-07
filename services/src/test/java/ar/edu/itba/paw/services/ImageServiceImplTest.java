package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.ImageMetadata;
import ar.edu.itba.paw.persistence.ImageDao;
import ar.edu.itba.paw.services.exceptions.imageUpload.*;
import java.io.ByteArrayInputStream;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ImageServiceImplTest {

    @Mock private ImageDao imageDao;

    @InjectMocks private ImageServiceImpl imageService;

    @Test
    public void testStoreWithValidImageDelegatesToDao() throws Exception {
        final byte[] content = pngContent();
        Mockito.when(
                        imageDao.create(
                                Mockito.eq("image/png"),
                                Mockito.eq((long) content.length),
                                Mockito.any()))
                .thenReturn(44L);

        final Long imageId =
                imageService.store("image/png", content.length, new ByteArrayInputStream(content));

        Assertions.assertEquals(44L, imageId);
    }

    @Test
    public void testStoreNormalizesContentTypeBeforeValidatingContent() throws Exception {
        final byte[] content = jpegContent();
        Mockito.when(
                        imageDao.create(
                                Mockito.eq("image/jpeg"),
                                Mockito.eq((long) content.length),
                                Mockito.any()))
                .thenReturn(45L);

        final Long imageId =
                imageService.store(
                        " IMAGE/JPG ", content.length, new ByteArrayInputStream(content));

        Assertions.assertEquals(45L, imageId);
    }

    @Test
    public void testStoreRejectsUnsupportedContentType() {
        Assertions.assertThrows(
                UnsupportedImageFormatException.class,
                () ->
                        imageService.store(
                                "application/pdf", 10, new ByteArrayInputStream(new byte[10])));
    }

    @Test
    public void testStoreRejectsContentThatDoesNotMatchDeclaredType() {
        final byte[] content = jpegContent();

        Assertions.assertThrows(
                UnsupportedImageFormatException.class,
                () ->
                        imageService.store(
                                "image/png", content.length, new ByteArrayInputStream(content)));
    }

    @Test
    public void testStoreRejectsAllowedTypeWithInvalidImageContent() {
        final byte[] content = new byte[] {1, 2, 3};

        Assertions.assertThrows(
                UnsupportedImageFormatException.class,
                () ->
                        imageService.store(
                                "image/png", content.length, new ByteArrayInputStream(content)));
    }

    @Test
    public void testResolveImageMetadataRejectsUnsupportedFilenameExtension() {
        final byte[] content = pngContent();
        final ImageUpload upload = imageUpload("image/png", "avatar.txt", content);

        Assertions.assertThrows(
                UnsupportedImageFormatException.class,
                () -> imageService.resolveImageMetadata(upload));
    }

    @Test
    public void testResolveImageMetadataReturnsNormalizedStoredMetadata() throws Exception {
        final byte[] content = jpegContent();
        final ImageUpload upload = imageUpload(" IMAGE/JPG ", "avatar.jpg", content, 100L);
        Mockito.when(
                        imageDao.create(
                                Mockito.eq("image/jpeg"),
                                Mockito.eq((long) content.length),
                                Mockito.any()))
                .thenReturn(46L);

        final ImageMetadata metadata = imageService.resolveImageMetadata(upload);

        Assertions.assertEquals(46L, metadata.getId());
        Assertions.assertEquals("image/jpeg", metadata.getContentType());
        Assertions.assertEquals(content.length, metadata.getContentLength());
    }

    @Test
    public void testFindMetadataByIdDelegatesToDao() {
        final ImageMetadata metadata = new ImageMetadata(7L, "image/jpeg", 100L);
        Mockito.when(imageDao.findMetadataById(7L)).thenReturn(Optional.of(metadata));

        final Optional<ImageMetadata> result = imageService.findMetadataById(7L);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("image/jpeg", result.get().getContentType());
    }

    private static byte[] pngContent() {
        return new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00};
    }

    private static byte[] jpegContent() {
        return new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};
    }

    private static ImageUpload imageUpload(
            final String contentType, final String originalFilename, final byte[] content) {
        return imageUpload(contentType, originalFilename, content, content.length);
    }

    private static ImageUpload imageUpload(
            final String contentType,
            final String originalFilename,
            final byte[] content,
            final long declaredContentLength) {
        return new ImageUpload() {
            @Override
            public String getContentType() {
                return contentType;
            }

            @Override
            public long getContentLength() {
                return declaredContentLength;
            }

            @Override
            public String getOriginalFilename() {
                return originalFilename;
            }

            @Override
            public ByteArrayInputStream getContentStream() {
                return new ByteArrayInputStream(content);
            }
        };
    }
}
