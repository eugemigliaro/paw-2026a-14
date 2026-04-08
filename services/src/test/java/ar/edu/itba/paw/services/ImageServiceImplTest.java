package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.ImageMetadata;
import ar.edu.itba.paw.persistence.ImageDao;
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
        final byte[] content = new byte[] {1, 2, 3};
        Mockito.when(imageDao.create(Mockito.eq("image/png"), Mockito.eq(3L), Mockito.any()))
                .thenReturn(44L);

        final Long imageId = imageService.store("image/png", 3L, new ByteArrayInputStream(content));

        Assertions.assertEquals(44L, imageId);
    }

    @Test
    public void testStoreRejectsUnsupportedContentType() {
        final IllegalArgumentException exception =
                Assertions.assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                imageService.store(
                                        "application/pdf",
                                        10,
                                        new ByteArrayInputStream(new byte[10])));

        Assertions.assertEquals("Unsupported image format.", exception.getMessage());
    }

    @Test
    public void testFindMetadataByIdDelegatesToDao() {
        final ImageMetadata metadata = new ImageMetadata(7L, "image/jpeg", 100L);
        Mockito.when(imageDao.findMetadataById(7L)).thenReturn(Optional.of(metadata));

        final Optional<ImageMetadata> result = imageService.findMetadataById(7L);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("image/jpeg", result.get().getContentType());
    }
}
