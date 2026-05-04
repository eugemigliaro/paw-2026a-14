package ar.edu.itba.paw.persistence;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import javax.sql.DataSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@Rollback
@Transactional
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class ImageJdbcDaoTest {

    @Autowired private ImageDao imageDao;
    @Autowired private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void setUp() {
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    public void shouldCreateImage_WhenValidDataIsProvided() throws Exception {
        final byte[] content = "image-content".getBytes(StandardCharsets.UTF_8);
        final String contentType = "image/png";

        final Long imageId =
                imageDao.create(contentType, content.length, new ByteArrayInputStream(content));

        Assertions.assertNotNull(imageId);
        Assertions.assertNotEquals(0L, imageId);

        final Long countInDb =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM images WHERE id = ? AND content_type = ? AND content_length = ?",
                        Long.class,
                        imageId,
                        contentType,
                        (long) content.length);
        Assertions.assertEquals(1L, countInDb, "Image should be persisted in database");
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

        final String pngType =
                jdbcTemplate.queryForObject(
                        "SELECT content_type FROM images WHERE id = ?", String.class, pngImageId);
        final String jpegType =
                jdbcTemplate.queryForObject(
                        "SELECT content_type FROM images WHERE id = ?", String.class, jpegImageId);

        Assertions.assertEquals("image/png", pngType);
        Assertions.assertEquals("image/jpeg", jpegType);
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

        final Long countInDb =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM images WHERE id = ?", Long.class, imageId);
        Assertions.assertEquals(1L, countInDb, "Image should still exist after streaming");
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
    }
}
