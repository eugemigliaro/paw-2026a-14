package ar.edu.itba.paw.webapp.config;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class WebConfigFlywayTest {

    @Test
    void flywayDisablesOutOfOrderMigrationsByDefault() {
        final WebConfig webConfig = webConfig(false);

        final Flyway flyway = webConfig.flyway(Mockito.mock(DataSource.class));

        Assertions.assertFalse(flyway.getConfiguration().isOutOfOrder());
    }

    @Test
    void flywayEnablesOutOfOrderMigrationsForControlledBackfill() {
        final WebConfig webConfig = webConfig(true);

        final Flyway flyway = webConfig.flyway(Mockito.mock(DataSource.class));

        Assertions.assertTrue(flyway.getConfiguration().isOutOfOrder());
    }

    private static WebConfig webConfig(final boolean flywayOutOfOrder) {
        return new WebConfig(
                null, "jdbc:postgresql://localhost/paw", "paw", "paw", flywayOutOfOrder);
    }
}
