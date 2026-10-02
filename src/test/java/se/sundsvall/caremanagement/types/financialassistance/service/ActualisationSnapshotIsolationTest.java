package se.sundsvall.caremanagement.types.financialassistance.service;

import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.attachments.service.AttachmentService;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.decisions.service.DecisionService;
import se.sundsvall.caremanagement.lifecare.service.ActualisationResult;
import se.sundsvall.caremanagement.lifecare.service.ActualisationService;
import se.sundsvall.caremanagement.types.financialassistance.api.model.ActualisationRequest;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;
import static org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED;

/**
 * The actualisation step commits its retry marker on the errand row in a transaction of its own, and then stores the
 * insats the actualisation was linked to on the same row. MariaDB with {@code innodb_snapshot_isolation} on — the
 * default from 11.6.2, and what Drakel runs — refuses a write to a row that another transaction changed after the
 * writer's snapshot was taken ({@code 1020 Record has changed since last read}). While the whole step ran in one
 * transaction that had read the row before the marker was committed, every first attempt failed that way after
 * Lifecare had created the actualisation and taken the application, and the retry adopted the actualisation and
 * uploaded the application a second time (EB-26100003, 2 Oct 2026).
 *
 * <p>
 * Needs a real database with snapshot isolation switched on, and no transaction of the test's own. The junit database
 * is MariaDB 10.6, where the setting exists but is off, so every connection here switches it on.
 */
@DataJpaTest(properties = "spring.datasource.hikari.connection-init-sql=SET SESSION innodb_snapshot_isolation = ON")
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("junit")
@Transactional(propagation = NOT_SUPPORTED)
@Import(FinancialAssistanceActualisationService.class)
class ActualisationSnapshotIsolationTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "FINANCIAL_ASSISTANCE";
	private static final String ERRAND_ID = "7c2e3f50-3333-4e4d-9f70-2c3d4e5f6071";
	private static final String APPLICANT = "1d2e3f40-4444-4a5b-8c6d-3e4f5a6b7c8d";

	@MockitoBean
	private ActualisationService actualisationService;

	@MockitoBean
	private AttachmentService attachmentService;

	@MockitoBean
	private DecisionService decisionService;

	@MockitoBean
	private ErrandService errandService;

	@Autowired
	private FinancialAssistanceRepository repository;

	@Autowired
	private FinancialAssistanceActualisationService service;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		repository.saveAndFlush(FinancialAssistanceEntity.create().withErrandId(ERRAND_ID).withApplicationType("RENEWAL"));
	}

	@AfterEach
	void tearDown() {
		repository.deleteById(ERRAND_ID);
	}

	@Test
	void theConnectionsRunWithSnapshotIsolation() {
		assertThat(jdbcTemplate.queryForObject("select @@session.innodb_snapshot_isolation", Integer.class)).isOne();
	}

	@Test
	void theFirstAttemptStoresTheInsatsOnTheRowItMarked() {
		when(actualisationService.createActualisation(eq(MUNICIPALITY_ID), eq(APPLICANT), any(LocalDate.class), eq(false)))
			.thenReturn(new ActualisationResult(173, "kal18rex", 25));

		final var response = service.createActualisation(MUNICIPALITY_ID, NAMESPACE, ActualisationRequest.create()
			.withErrandId(ERRAND_ID)
			.withApplicant(APPLICANT)
			.withApplicationMonth("2026-11"));

		assertThat(response.getActualisationId()).isEqualTo(173);
		assertThat(repository.findByErrandId(ERRAND_ID)).hasValueSatisfying(stored -> {
			assertThat(stored.getLifecareServiceId()).isEqualTo(25);
			assertThat(stored.getActualisationRequestedAt()).isNotNull();
		});
		verify(actualisationService, never()).createOrAdoptActualisation(any(), any(), any(), anyBoolean(), any());
		verify(decisionService).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any());
	}
}
