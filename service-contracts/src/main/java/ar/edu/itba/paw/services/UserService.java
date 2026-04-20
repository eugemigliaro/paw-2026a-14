package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.User;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public interface UserService {

    User createUser(final String email, final String username);

    Optional<User> findByEmail(final String email);

    Optional<User> findById(final Long id);

    Optional<User> findByUsername(final String username);

    User updateProfile(
            Long id,
            String username,
            String name,
            String lastName,
            String phone,
            String profileImageContentType,
            long profileImageContentLength,
            InputStream profileImageContentStream)
            throws IOException;

    User updateProfileImage(
            Long id, String contentType, long contentLength, InputStream contentStream)
            throws IOException;
}
