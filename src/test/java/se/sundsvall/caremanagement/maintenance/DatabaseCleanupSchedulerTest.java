package se.sundsvall.caremanagement.maintenance;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatabaseCleanupSchedulerTest {

	@Mock
	private JdbcTemplate jdbcTemplateMock;

	@Mock
	private Connection connectionMock;

	@Mock
	private Statement statementMock;

	@Mock
	private ResultSet resultSetMock;

	@InjectMocks
	private DatabaseCleanupScheduler scheduler;

	@Test
	void resetDemoDataWipesNonPreservedTablesAndTogglesForeignKeyChecks() throws SQLException {
		when(jdbcTemplateMock.execute(ArgumentMatchers.<ConnectionCallback<Void>>any()))
			.thenAnswer(invocation -> invocation.<ConnectionCallback<Void>>getArgument(0).doInConnection(connectionMock));
		when(connectionMock.createStatement()).thenReturn(statementMock);
		when(statementMock.executeQuery(anyString())).thenReturn(resultSetMock);
		// information_schema yields: errand (wipe), namespace_config (preserved), errand_note (wipe)
		when(resultSetMock.next()).thenReturn(true, true, true, false);
		when(resultSetMock.getString(1)).thenReturn("errand", "namespace_config", "errand_note");
		when(statementMock.executeUpdate(anyString())).thenReturn(3);

		scheduler.resetDemoData();

		verify(statementMock).execute("SET FOREIGN_KEY_CHECKS = 0");
		verify(statementMock).execute("SET FOREIGN_KEY_CHECKS = 1");
		verify(statementMock).executeUpdate("DELETE FROM `errand`");
		verify(statementMock).executeUpdate("DELETE FROM `errand_note`");
		verify(statementMock, never()).executeUpdate("DELETE FROM `namespace_config`");
	}
}
