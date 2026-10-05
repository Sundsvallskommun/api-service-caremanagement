package se.sundsvall.caremanagement.types.financialassistance.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.lifecare.service.LifecareCaseHistoryService;
import se.sundsvall.caremanagement.lifecare.service.model.DecisionView;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CalculationDraft;
import se.sundsvall.caremanagement.types.financialassistance.configuration.DecisionProposalProperties;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FaWarningRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaWarningEntity;
import se.sundsvall.dept44.problem.Problem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;

/**
 * The decision proposal over a real {@link WarningService} and an in-memory warning store, across runs: a failed
 * återkrav read must neither hide nor close a claim already shown, and the next successful read closes the read-failure
 * warning. The unit tests of each class mock the other; this checks what the handläggare actually sees run after run.
 */
@ExtendWith(MockitoExtension.class)
class DecisionProposalLifecareReadFailureTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = "errand-1";
	private static final String APPLICANT = "pnr-a";
	private static final LocalDate PREVIOUS_DECISION_FROM = LocalDate.parse("2025-06-01");
	private static final LocalDate RECOVERY_CLAIMS_FROM = LocalDate.parse("2023-06-01");
	private static final LocalDate TO = LocalDate.parse("2026-06-30");

	@Mock
	private ProposalBasisService proposalBasisServiceMock;

	@Mock
	private LifecareCaseHistoryService lifecareCaseHistoryServiceMock;

	@Mock
	private FaWarningRepository warningRepositoryMock;

	private final List<FaWarningEntity> store = new ArrayList<>();

	private DecisionProposalService service;

	@BeforeEach
	void setUp() {
		when(warningRepositoryMock.findByErrandId(ERRAND_ID)).thenAnswer(_ -> List.copyOf(store));
		when(warningRepositoryMock.save(any(FaWarningEntity.class))).thenAnswer(invocation -> {
			final FaWarningEntity entity = invocation.getArgument(0);
			if (store.stream().noneMatch(stored -> stored == entity)) {
				store.add(entity);
			}
			return entity;
		});
		// The in-memory store keeps (type, sourceKey) unique per errand, as the table does.
		doAnswer(invocation -> {
			final String type = invocation.getArgument(2);
			final String sourceKey = invocation.getArgument(3);
			if (store.stream().noneMatch(stored -> type.equals(stored.getType()) && sourceKey.equals(stored.getSourceKey()))) {
				store.add(FaWarningEntity.create().withId(invocation.getArgument(0)).withErrandId(invocation.getArgument(1))
					.withType(type).withSourceKey(sourceKey).withMessage(invocation.getArgument(4)).withStatus(invocation.getArgument(5)));
			}
			return null;
		}).when(warningRepositoryMock).insertIgnore(any(), any(), any(), any(), any(), any(), any());
		final var household = new HouseholdPartyService.Household(Optional.of(APPLICANT), false, Optional.empty(), Optional.empty());
		final var draft = CalculationDraft.create().withApplicationMonth("2026-06").withIncomeSum(BigDecimal.ZERO);
		when(proposalBasisServiceMock.basis(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(new ProposalBasisService.ProposalBasis(draft, household,
			Optional.of(YearMonth.parse("2026-06")), Optional.of(new BigDecimal("6200")), Optional.of(new BigDecimal("6200")), Optional.of(ProposalBasisService.AMOUNT_BASIS_ESTIMATE), Optional.empty(),
			Optional.empty()));
		service = new DecisionProposalService(proposalBasisServiceMock, lifecareCaseHistoryServiceMock, new WarningService(warningRepositoryMock),
			new LifecareDecisionFilter(Set.of(2)), new DecisionProposalProperties(36));
	}

	private static DecisionView recoveryClaim() {
		return new DecisionView(41, "2024-02-10", "EK Återkrav", null, null, "", "Anna", "IFO", 2, new BigDecimal("3200"), null, null, List.of());
	}

	@Test
	void aFailedReadKeepsTheClaimOpenAndTheNextSuccessfulReadClosesTheFailure() {
		when(lifecareCaseHistoryServiceMock.listDecisions(MUNICIPALITY_ID, APPLICANT, PREVIOUS_DECISION_FROM, TO)).thenReturn(List.of());
		when(lifecareCaseHistoryServiceMock.listDecisions(MUNICIPALITY_ID, APPLICANT, RECOVERY_CLAIMS_FROM, TO))
			.thenReturn(List.of(recoveryClaim()))
			.thenThrow(Problem.valueOf(BAD_GATEWAY, "Lifecare down"))
			.thenReturn(List.of(recoveryClaim()));

		// Run 1: the claim is read and shown.
		assertThat(service.get(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID).getWarnings())
			.extracting("type", "status").containsExactly(tuple("RECOVERY_CLAIM", "OPEN"));

		// Run 2: the read fails — the claim stays open and the failure is shown next to it on the DECISION tab.
		assertThat(service.get(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID).getWarnings())
			.extracting("type", "sourceKey", "status", "section").containsExactlyInAnyOrder(
				tuple("RECOVERY_CLAIM", "recovery-claim:41", "OPEN", "DECISION"),
				tuple("LIFECARE_READ_FAILED", "lifecare-read:recovery-claims", "OPEN", "DECISION"));

		// Run 3: the read succeeds — the failure closes itself, the claim is still open.
		assertThat(service.get(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID).getWarnings())
			.extracting("type", "status", "autoResolved").containsExactlyInAnyOrder(
				tuple("RECOVERY_CLAIM", "OPEN", false),
				tuple("LIFECARE_READ_FAILED", "CLOSED", true));
	}

	@Test
	void aClaimFirstSeenAfterAnOutageIsRaised() {
		when(lifecareCaseHistoryServiceMock.listDecisions(MUNICIPALITY_ID, APPLICANT, PREVIOUS_DECISION_FROM, TO)).thenReturn(List.of());
		when(lifecareCaseHistoryServiceMock.listDecisions(MUNICIPALITY_ID, APPLICANT, RECOVERY_CLAIMS_FROM, TO))
			.thenThrow(Problem.valueOf(BAD_GATEWAY, "Lifecare down"))
			.thenReturn(List.of(recoveryClaim()));

		assertThat(service.get(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID).getWarnings())
			.extracting("type", "status").containsExactly(tuple("LIFECARE_READ_FAILED", "OPEN"));

		assertThat(service.get(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID).getWarnings())
			.extracting("type", "status").containsExactlyInAnyOrder(tuple("LIFECARE_READ_FAILED", "CLOSED"), tuple("RECOVERY_CLAIM", "OPEN"));
	}
}
