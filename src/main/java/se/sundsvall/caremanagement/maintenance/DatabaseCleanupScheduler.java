package se.sundsvall.caremanagement.maintenance;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.scheduling.Dept44Scheduled;

/**
 * Nightly demo reset: wipes every table except the seed/config and framework/system tables so a demo environment starts
 * from a clean slate each morning.
 *
 * <p>
 * <b>Destructive and disabled by default.</b> The bean is only created when
 * {@code maintenance.database-cleanup.enabled}
 * is {@code true}, so it never exists in mainline/production deployments — it is intended only for the throwaway demo
 * environment (where it is enabled together with a cron expression via environment configuration).
 * </p>
 *
 * <p>
 * The wipe is driven from {@code information_schema} (rather than a hard-coded table list) so future errand-type tables
 * are cleaned automatically. It runs entirely on a single pooled connection with foreign-key checks disabled —
 * {@code SET FOREIGN_KEY_CHECKS} is connection-scoped, so the toggle and the {@code DELETE}s must share one session.
 * The
 * flag is always restored before the connection returns to the pool.
 * </p>
 */
@Component
@ConditionalOnProperty(prefix = "maintenance.database-cleanup", name = "enabled", havingValue = "true")
class DatabaseCleanupScheduler {

	private static final Logger LOG = LoggerFactory.getLogger(DatabaseCleanupScheduler.class);

	/**
	 * Tables that survive the reset: seed/config data and framework/system tables. Everything else (errands and all their
	 * children, plus the Modulith event-publication outbox) is wiped.
	 */
	private static final Set<String> PRESERVED_TABLES = Set.of(
		"namespace_config",       // municipality + namespace configuration
		"lookup",                 // metadata / enum seed data
		"shedlock",               // distributed scheduler locks
		"flyway_schema_history"); // Flyway bookkeeping

	private static final String LIST_TABLES_SQL = "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE'";

	private final JdbcTemplate jdbcTemplate;

	DatabaseCleanupScheduler(final JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Dept44Scheduled(cron = "${scheduler.database-cleanup.cron}",
		name = "${scheduler.database-cleanup.name}",
		lockAtMostFor = "${scheduler.database-cleanup.shedlock-lock-at-most-for}",
		maximumExecutionTime = "${scheduler.database-cleanup.maximum-execution-time}")
	void resetDemoData() {
		jdbcTemplate.execute((ConnectionCallback<Void>) this::wipe);
	}

	private Void wipe(final Connection connection) throws SQLException {
		try (final var statement = connection.createStatement()) {
			statement.execute("SET FOREIGN_KEY_CHECKS = 0");
			try {
				final var tables = tablesToWipe(statement);
				var totalRows = 0;
				for (final var table : tables) {
					final var deleted = statement.executeUpdate("DELETE FROM `" + table + "`");
					totalRows += deleted;
					LOG.info("Wiped {} row(s) from {}", deleted, table);
				}
				LOG.info("Nightly demo reset complete: removed {} row(s) from {} table(s); preserved {}",
					totalRows, tables.size(), PRESERVED_TABLES);
			} finally {
				// Restore before the connection returns to the pool, or the next borrower inherits disabled
				// foreign-key checks.
				statement.execute("SET FOREIGN_KEY_CHECKS = 1");
			}
		}
		return null;
	}

	private List<String> tablesToWipe(final Statement statement) throws SQLException {
		final var tables = new ArrayList<String>();
		try (final var resultSet = statement.executeQuery(LIST_TABLES_SQL)) {
			while (resultSet.next()) {
				final var table = resultSet.getString(1);
				if (!PRESERVED_TABLES.contains(table)) {
					tables.add(table);
				}
			}
		}
		return tables;
	}
}
