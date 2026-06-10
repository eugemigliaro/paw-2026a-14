package ar.edu.itba.paw.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.hsqldb.jdbc.JDBCDriver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

class FlywayOutOfOrderMigrationTest {

    @TempDir Path migrationDirectory;

    @Test
    void shouldApplyReservedVersionInOrderOnFreshDatabase() throws IOException {
        writeMigration("V1__create_marker.sql", "CREATE TABLE marker (id INTEGER PRIMARY KEY);");
        writeMigration("V16__reserve_migration_version.sql", "VALUES (1);");
        writeMigration("V17__add_marker.sql", "INSERT INTO marker (id) VALUES (17);");
        final Flyway flyway = flyway(dataSource(), false);

        flyway.migrate();

        Assertions.assertEquals(List.of("1", "16", "17"), appliedVersions(flyway));
        Assertions.assertEquals(MigrationState.SUCCESS, migration(flyway, "16").getState());
    }

    @Test
    void shouldBackfillReservedVersionAfterLaterVersionsWereApplied() throws IOException {
        writeMigration("V1__create_marker.sql", "CREATE TABLE marker (id INTEGER PRIMARY KEY);");
        writeMigration("V17__add_marker.sql", "INSERT INTO marker (id) VALUES (17);");
        final DataSource dataSource = dataSource();
        flyway(dataSource, false).migrate();
        writeMigration("V16__reserve_migration_version.sql", "VALUES (1);");
        final Flyway flyway = flyway(dataSource, true);

        flyway.migrate();

        Assertions.assertEquals(List.of("1", "17", "16"), appliedVersions(flyway));
        Assertions.assertEquals(MigrationState.OUT_OF_ORDER, migration(flyway, "16").getState());
    }

    private Flyway flyway(final DataSource dataSource, final boolean outOfOrder) {
        final String location =
                "filesystem:" + migrationDirectory.toAbsolutePath().toString().replace('\\', '/');
        return Flyway.configure()
                .dataSource(dataSource)
                .locations(location)
                .outOfOrder(outOfOrder)
                .load();
    }

    private static DataSource dataSource() {
        final SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
        dataSource.setDriverClass(JDBCDriver.class);
        dataSource.setUrl("jdbc:hsqldb:mem:v16-" + UUID.randomUUID());
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    private void writeMigration(final String filename, final String sql) throws IOException {
        Files.writeString(migrationDirectory.resolve(filename), sql);
    }

    private static List<String> appliedVersions(final Flyway flyway) {
        return Arrays.stream(flyway.info().applied())
                .map(info -> info.getVersion().toString())
                .collect(Collectors.toList());
    }

    private static MigrationInfo migration(final Flyway flyway, final String version) {
        return Arrays.stream(flyway.info().applied())
                .filter(info -> version.equals(info.getVersion().toString()))
                .findFirst()
                .orElseThrow();
    }
}
