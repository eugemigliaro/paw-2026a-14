package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.UserRole;
import ar.edu.itba.paw.persistence.UserDao;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PropertySource("classpath:/application.properties")
public class AdminBootstrapServiceImpl implements AdminBootstrapService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminBootstrapServiceImpl.class);

    private final UserDao userDao;
    private final Clock clock;
    private final String email;
    private final String username;
    private final String name;
    private final String lastName;
    private final String passwordHash;

    public AdminBootstrapServiceImpl(
            final UserDao userDao,
            final Clock clock,
            @Value("${bootstrap.admin.email:}") final String email,
            @Value("${bootstrap.admin.username:}") final String username,
            @Value("${bootstrap.admin.name:}") final String name,
            @Value("${bootstrap.admin.lastName:}") final String lastName,
            @Value("${bootstrap.admin.passwordHash:}") final String passwordHash) {
        this.userDao = Objects.requireNonNull(userDao);
        this.clock = Objects.requireNonNull(clock);
        this.email = email == null ? "" : email.trim().toLowerCase();
        this.username = username == null ? "" : username.trim().toLowerCase();
        this.name = name == null ? "" : name.trim();
        this.lastName = lastName == null ? "" : lastName.trim();
        this.passwordHash = passwordHash == null ? "" : passwordHash.trim();
    }

    @Override
    @Transactional
    public void bootstrapFromConfiguration() {
        if (isInvalidBootstrapValue(email)
                || isInvalidBootstrapValue(username)
                || isInvalidBootstrapValue(name)
                || isInvalidBootstrapValue(lastName)
                || isInvalidBootstrapValue(passwordHash)) {
            LOGGER.info("Admin bootstrap skipped because configuration is incomplete");
            return;
        }
        if (userDao.findAccountByEmail(email).isPresent()) {
            LOGGER.info("Admin bootstrap skipped because account already exists email={}", email);
            return;
        }
        if (userDao.findByUsername(username).isPresent()) {
            LOGGER.warn("Admin bootstrap skipped due to username collision username={}", username);
            return;
        }

        userDao.createAccount(
                email,
                username,
                name,
                lastName,
                null,
                passwordHash,
                UserRole.ADMIN_MOD,
                Instant.now(clock));
        LOGGER.info("Admin bootstrap created account email={}", email);
    }

    private static boolean isInvalidBootstrapValue(final String value) {
        return value == null || value.isBlank() || value.contains("${");
    }
}
