package ar.edu.itba.paw.persistence;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
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
public class ImageJdbcDaoTest {

    @Autowired private ImageDao imageDao;

    @Test
    public void testCreateAndStreamImage() throws Exception {
        final byte[] expectedContent = "fake-image-content".getBytes(StandardCharsets.UTF_8);

        final Long imageId =
                imageDao.create(
                        "image/png",
                        expectedContent.length,
                        new ByteArrayInputStream(expectedContent));

        final var metadata = imageDao.findMetadataById(imageId).orElseThrow();

        Assertions.assertEquals("image/png", metadata.getContentType());
        Assertions.assertEquals(expectedContent.length, metadata.getContentLength());

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final boolean streamed = imageDao.streamContentById(imageId, outputStream);

        Assertions.assertTrue(streamed);
        Assertions.assertArrayEquals(expectedContent, outputStream.toByteArray());
    }
}
