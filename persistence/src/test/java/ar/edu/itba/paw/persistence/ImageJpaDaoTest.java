package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.Image;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@Rollback
@Transactional
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class ImageJpaDaoTest {

    @Autowired private ImageDao imageDao;

    @PersistenceContext private EntityManager entityManager;

    @Test
    public void shouldCreateImage_WhenValidDataIsProvided() throws Exception {
        final byte[] content = "image-content".getBytes(StandardCharsets.UTF_8);
        final String contentType = "image/png";

        final Long imageId =
                imageDao.create(contentType, content.length, new ByteArrayInputStream(content));

        Assertions.assertNotNull(imageId);
        Assertions.assertNotEquals(0L, imageId);

        final Image persistedImage = entityManager.find(Image.class, imageId);
        Assertions.assertNotNull(persistedImage, "Image should be persisted in database");
        Assertions.assertEquals(contentType, persistedImage.getContentType());
        Assertions.assertEquals((long) content.length, persistedImage.getContentLength());
        Assertions.assertArrayEquals(content, persistedImage.getContent());
        Assertions.assertNotNull(persistedImage.getCreatedAt());
    }

    @Test
    public void shouldCreateImage_WithDifferentContentTypes() throws Exception {
        final byte[] pngContent = "png-data".getBytes(StandardCharsets.UTF_8);
        final byte[] jpegContent = "jpeg-data".getBytes(StandardCharsets.UTF_8);

        final Long pngImageId =
                imageDao.create(
                        "image/png", pngContent.length, new ByteArrayInputStream(pngContent));
        final Long jpegImageId =
                imageDao.create(
                        "image/jpeg", jpegContent.length, new ByteArrayInputStream(jpegContent));

        final Image pngImage = entityManager.find(Image.class, pngImageId);
        final Image jpegImage = entityManager.find(Image.class, jpegImageId);

        Assertions.assertEquals("image/png", pngImage.getContentType());
        Assertions.assertEquals("image/jpeg", jpegImage.getContentType());
    }

    @Test
    public void shouldFindMetadataById_WhenImageExists() throws Exception {
        final byte[] content = "test-content".getBytes(StandardCharsets.UTF_8);
        final String contentType = "image/gif";
        final Long imageId =
                imageDao.create(contentType, content.length, new ByteArrayInputStream(content));

        final var metadata = imageDao.findMetadataById(imageId);

        Assertions.assertTrue(metadata.isPresent(), "Metadata should be found");
        Assertions.assertEquals(imageId, metadata.get().getId());
        Assertions.assertEquals(contentType, metadata.get().getContentType());
        Assertions.assertEquals(content.length, metadata.get().getContentLength());

        final Image persistedImage = entityManager.find(Image.class, imageId);
        Assertions.assertNotNull(persistedImage);
        Assertions.assertEquals(contentType, persistedImage.getContentType());
    }

    @Test
    public void shouldFindMetadataById_WhenImageNotFound() {
        final Long nonExistentImageId = 99999L;

        final var metadata = imageDao.findMetadataById(nonExistentImageId);

        Assertions.assertTrue(
                metadata.isEmpty(), "Metadata should not be found for non-existent image ID");
    }

    @Test
    public void shouldFindMetadataById_WithMultipleImages() throws Exception {
        final byte[] content1 = "content1".getBytes(StandardCharsets.UTF_8);
        final byte[] content2 = "content2-longer".getBytes(StandardCharsets.UTF_8);
        final Long imageId1 =
                imageDao.create("image/png", content1.length, new ByteArrayInputStream(content1));
        final Long imageId2 =
                imageDao.create("image/jpeg", content2.length, new ByteArrayInputStream(content2));

        final var metadata1 = imageDao.findMetadataById(imageId1);
        final var metadata2 = imageDao.findMetadataById(imageId2);

        Assertions.assertEquals(imageId1, metadata1.get().getId());
        Assertions.assertEquals(content1.length, metadata1.get().getContentLength());
        Assertions.assertEquals(imageId2, metadata2.get().getId());
        Assertions.assertEquals(content2.length, metadata2.get().getContentLength());

        final Image image1 = entityManager.find(Image.class, imageId1);
        final Image image2 = entityManager.find(Image.class, imageId2);
        Assertions.assertNotNull(image1);
        Assertions.assertNotNull(image2);
        Assertions.assertEquals(content1.length, image1.getContentLength());
        Assertions.assertEquals(content2.length, image2.getContentLength());
    }

    @Test
    public void shouldStreamContentById_WhenImageExists() throws Exception {
        final byte[] expectedContent = "my-image-data".getBytes(StandardCharsets.UTF_8);
        final Long imageId =
                imageDao.create(
                        "image/png",
                        expectedContent.length,
                        new ByteArrayInputStream(expectedContent));
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        final boolean result = imageDao.streamContentById(imageId, outputStream);

        Assertions.assertTrue(result, "streamContentById should return true for existing image");
        Assertions.assertArrayEquals(expectedContent, outputStream.toByteArray());

        final Image persistedImage = entityManager.find(Image.class, imageId);
        Assertions.assertNotNull(persistedImage);
        Assertions.assertArrayEquals(expectedContent, persistedImage.getContent());
    }

    @Test
    public void shouldStreamContentById_WhenImageNotFound() throws Exception {
        final Long nonExistentImageId = 99999L;
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        final boolean result = imageDao.streamContentById(nonExistentImageId, outputStream);

        Assertions.assertFalse(
                result, "streamContentById should return false for non-existent image");
        Assertions.assertEquals(0, outputStream.size(), "Output stream should be empty");
    }

    @Test
    public void shouldStreamContentById_WithLargeContent() throws Exception {
        final byte[] largeContent = new byte[1024 * 1024];
        for (int i = 0; i < largeContent.length; i++) {
            largeContent[i] = (byte) (i % 256);
        }
        final Long imageId =
                imageDao.create(
                        "image/png", largeContent.length, new ByteArrayInputStream(largeContent));
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        final boolean result = imageDao.streamContentById(imageId, outputStream);

        Assertions.assertTrue(result);
        Assertions.assertArrayEquals(largeContent, outputStream.toByteArray());

        final Image persistedImage = entityManager.find(Image.class, imageId);
        Assertions.assertNotNull(persistedImage);
        Assertions.assertEquals(largeContent.length, persistedImage.getContentLength());
    }

    @Test
    public void shouldStreamContentById_MultipleTimesWithSameImage() throws Exception {
        final byte[] content = "reusable-image".getBytes(StandardCharsets.UTF_8);
        final Long imageId =
                imageDao.create("image/png", content.length, new ByteArrayInputStream(content));

        final ByteArrayOutputStream output1 = new ByteArrayOutputStream();
        final ByteArrayOutputStream output2 = new ByteArrayOutputStream();
        final boolean result1 = imageDao.streamContentById(imageId, output1);
        final boolean result2 = imageDao.streamContentById(imageId, output2);

        Assertions.assertTrue(result1);
        Assertions.assertTrue(result2);
        Assertions.assertArrayEquals(content, output1.toByteArray());
        Assertions.assertArrayEquals(content, output2.toByteArray());

        final Image persistedImage = entityManager.find(Image.class, imageId);
        Assertions.assertNotNull(persistedImage);
        Assertions.assertArrayEquals(content, persistedImage.getContent());
    }

    @Test
    public void shouldStreamContentById_WithDifferentImages() throws Exception {
        final byte[] content1 = "image-one".getBytes(StandardCharsets.UTF_8);
        final byte[] content2 = "image-two".getBytes(StandardCharsets.UTF_8);
        final Long imageId1 =
                imageDao.create("image/png", content1.length, new ByteArrayInputStream(content1));
        final Long imageId2 =
                imageDao.create("image/jpeg", content2.length, new ByteArrayInputStream(content2));

        final ByteArrayOutputStream output1 = new ByteArrayOutputStream();
        final ByteArrayOutputStream output2 = new ByteArrayOutputStream();
        final boolean result1 = imageDao.streamContentById(imageId1, output1);
        final boolean result2 = imageDao.streamContentById(imageId2, output2);

        Assertions.assertTrue(result1);
        Assertions.assertTrue(result2);
        Assertions.assertArrayEquals(content1, output1.toByteArray());
        Assertions.assertArrayEquals(content2, output2.toByteArray());

        final Image image1 = entityManager.find(Image.class, imageId1);
        final Image image2 = entityManager.find(Image.class, imageId2);
        Assertions.assertNotNull(image1);
        Assertions.assertNotNull(image2);
        Assertions.assertEquals("image/png", image1.getContentType());
        Assertions.assertEquals("image/jpeg", image2.getContentType());
    }
}
