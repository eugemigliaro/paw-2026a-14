package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.ImageMetadata;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

@Repository
public class ImageJdbcDao implements ImageDao {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public ImageJdbcDao(@NonNull final DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public Long create(
            final String contentType, final long contentLength, final InputStream contentStream)
            throws IOException {
        final KeyHolder keyHolder = new GeneratedKeyHolder();
        final Timestamp now = Timestamp.from(Instant.now());

        jdbcTemplate.update(
                connection -> {
                    final PreparedStatement statement =
                            connection.prepareStatement(
                                    "INSERT INTO images (content_type, content_length, content, created_at)"
                                            + " VALUES (?, ?, ?, ?)",
                                    new String[] {"id"});
                    statement.setString(1, contentType);
                    statement.setLong(2, contentLength);
                    statement.setBinaryStream(3, contentStream, contentLength);
                    statement.setTimestamp(4, now);
                    return statement;
                },
                keyHolder);

        final Number id = keyHolder.getKey();
        if (id == null) {
            throw new IllegalStateException("Image insert did not return an id");
        }
        return id.longValue();
    }

    @Override
    public Optional<ImageMetadata> findMetadataById(final Long imageId) {
        return jdbcTemplate
                .query(
                        "SELECT id, content_type, content_length FROM images WHERE id = ?",
                        (ResultSet rs, int rowNum) ->
                                new ImageMetadata(
                                        rs.getLong("id"),
                                        rs.getString("content_type"),
                                        rs.getLong("content_length")),
                        imageId)
                .stream()
                .findFirst();
    }

    @Override
    public boolean streamContentById(final Long imageId, final OutputStream outputStream)
            throws IOException {
        try {
            return Boolean.TRUE.equals(
                    jdbcTemplate.query(
                            "SELECT content FROM images WHERE id = ?",
                            (ResultSetExtractor<Boolean>) rs -> streamSingleRow(rs, outputStream),
                            imageId));
        } catch (final UncheckedIOException exception) {
            throw exception.getCause();
        }
    }

    private static Boolean streamSingleRow(final ResultSet rs, final OutputStream outputStream)
            throws java.sql.SQLException {
        if (!rs.next()) {
            return Boolean.FALSE;
        }

        try (InputStream inputStream = rs.getBinaryStream("content")) {
            if (inputStream == null) {
                return Boolean.FALSE;
            }

            final byte[] buffer = new byte[8192];
            int read;
            try {
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }
                outputStream.flush();
            } catch (final IOException exception) {
                throw new UncheckedIOException(exception);
            }
            return Boolean.TRUE;
        } catch (final IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
