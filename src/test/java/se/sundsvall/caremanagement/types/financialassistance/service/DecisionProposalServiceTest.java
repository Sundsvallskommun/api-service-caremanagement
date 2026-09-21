package se.sundsvall.caremanagement.types.financialassistance.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.lifecare.service.LifecareCaseHistoryService;
import se.sundsvall.caremanagement.lifecare.service.model.DecisionView;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CalculationDraft;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormExpenseRow;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormPersonRow;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Warning;
import se.sundsvall.dept44.problem.Problem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static se.sundsvall.caremanagement.types.financialassistance.service.DecisionProposalService.DEFAULT_REASON_OPTIONS;
import static se.sundsvall.caremanagement.types.financialassistance.service.WarningService.DECISION_PROPOSAL_TYPES;

@ExtendWith(MockitoExtension.class)
class DecisionProposalServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = "errand-1";
	private static final String APPLICANT = "pnr-a";
	private static final YearMonth MONTH = YearMonth.parse("2026-06");

	@Mock
	private ProposalBasisService proposalBasisServiceMock;

	@Mock
	private LifecareCaseHistoryService lifecareCaseHistoryServiceMock;

	@Mock
	private WarningService warningServiceMock;

	@InjectMocks
	private DecisionProposalService service;

	private static CalculationDraft draft() {
		return CalculationDraft.create()
			.withApplicationMonth("2026-06")
			.withCalculationFromDate(LocalDate.parse("2026-06-01"))
			.withCalculationToDate(LocalDate.parse("2026-06-30"))
			.withIncomeSum(new BigDecimal("3000"))
			.withExpenseSum(new BigDecimal("800"))
			.withSpecialExpenseSum(new BigDecimal("250"));
	}

	private static ProposalBasisService.ProposalBasis basis(final CalculationDraft draft, final Optional<String> applicant, final Optional<BigDecimal> normSum) {
		final var household = new HouseholdPartyService.Household(applicant, false, Optional.empty(), Optional.empty());
		return new ProposalBasisService.ProposalBasis(draft, household, Optional.of(MONTH), normSum, normSum.map(norm -> norm.add(new BigDecimal("1050")).subtract(new BigDecimal("3000"))));
	}

	private static DecisionView decision(final String type, final String reason) {
		return decision(type, reason, null, null);
	}

	private static DecisionView decision(final String type, final String reason, final String coApplicant, final String coApplicantReason) {
		return new DecisionView(1, "2026-04-28", type, "2026-05-01", "2026-05-31", reason, "Anna", "IFO", new BigDecimal("8500"), coApplicant, coApplicantReason, List.of());
	}

	@Test
	void bifallWithPreviousReasonAndChildren() {
		final var draft = draft().withPersons(List.of(NormPersonRow.create().withRole("CHILD").withIncluded(true)));
		when(proposalBasisServiceMock.basis(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(basis(draft, Optional.of(APPLICANT), Optional.of(new BigDecimal("6200"))));
		when(lifecareCaseHistoryServiceMock.listDecisions(APPLICANT, LocalDate.parse("2025-06-01"), LocalDate.parse("2026-06-30")))
			.thenReturn(List.of(decision("Bifall", "Boendekostnad"), decision("Avslag", "Äldre")));
		when(warningServiceMock.reconcileByTypes(ERRAND_ID, DECISION_PROPOSAL_TYPES, List.of())).thenReturn(List.of());

		final var proposal = service.get(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(proposal.getOutcome()).isEqualTo("BIFALL");
		assertThat(proposal.getOutcomeOptions()).extracting("code").containsExactly("BIFALL", "DELAVSLAG", "AVSLAG");
		assertThat(proposal.getPeriodFrom()).isEqualTo(LocalDate.parse("2026-06-01"));
		assertThat(proposal.getPeriodTo()).isEqualTo(LocalDate.parse("2026-06-30"));
		assertThat(proposal.getConcernedMonth()).isEqualTo("2026-06");
		assertThat(proposal.getEstimatedAmount()).isEqualByComparingTo("4250");
		assertThat(proposal.getNormSum()).isEqualByComparingTo("6200");
		assertThat(proposal.getIncomeSum()).isEqualByComparingTo("3000");
		assertThat(proposal.getExpenseSum()).isEqualByComparingTo("800");
		assertThat(proposal.getSpecialExpenseSum()).isEqualByComparingTo("250");
		assertThat(proposal.getExplanation()).isNull();
		assertThat(proposal.getReason()).isEqualTo("Boendekostnad");
		assertThat(proposal.getReasonOptions()).containsExactlyElementsOf(withPreviousReason("Boendekostnad")); // outside the catalogue → appended
		assertThat(proposal.getCoApplicantReason()).isNull(); // the decision had no co-applicant
		assertThat(proposal.getPhraseText()).isEqualTo("Bifall månad med barn");
		assertThat(proposal.getPreviousDecision().getType()).isEqualTo("Bifall");
		assertThat(proposal.getWarnings()).isEmpty();
	}

	@Test
	void delavslagRaisesOneWarningPerPartiallyRejectedExpenseAndAdvanceWarning() {
		final var draft = draft().withExpenses(List.of(
			NormExpenseRow.create().withCostType("RENT").withAppliedAmount(new BigDecimal("9000")).withEffectiveAmount(new BigDecimal("8000")),
			NormExpenseRow.create().withCostType("OTHER").withOtherSubType("Busskort").withAppliedAmount(new BigDecimal("500"))));
		when(proposalBasisServiceMock.basis(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(basis(draft, Optional.of(APPLICANT), Optional.of(new BigDecimal("6200"))));
		when(lifecareCaseHistoryServiceMock.listDecisions(eq(APPLICANT), eq(LocalDate.parse("2025-06-01")), eq(LocalDate.parse("2026-06-30"))))
			.thenReturn(List.of(decision("Förskott på förmån", "Arbetslös, ingen ersättning/stöd")));
		final var reconciled = List.of(Warning.create().withType("EXPENSE_PARTIALLY_REJECTED"));
		final var captor = ArgumentCaptor.forClass(List.class);
		when(warningServiceMock.reconcileByTypes(eq(ERRAND_ID), eq(DECISION_PROPOSAL_TYPES), captor.capture())).thenReturn(reconciled);

		final var proposal = service.get(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(proposal.getOutcome()).isEqualTo("DELAVSLAG");
		assertThat(proposal.getPhraseText()).isEqualTo("Bifall månad utan barn");
		assertThat(proposal.getReason()).isEqualTo("Arbetslös, ingen ersättning/stöd");
		assertThat(proposal.getReasonOptions()).containsExactlyElementsOf(DEFAULT_REASON_OPTIONS); // already in the catalogue → no duplicate
		assertThat(proposal.getWarnings()).isSameAs(reconciled);
		@SuppressWarnings("unchecked")
		final List<WarningService.WarningInput> inputs = captor.getValue();
		assertThat(inputs).extracting(WarningService.WarningInput::type, WarningService.WarningInput::sourceKey, WarningService.WarningInput::message).containsExactly(
			tuple("PREVIOUS_DECISION_ADVANCE_ON_BENEFIT", "previous-decision", "Föregående beslut i Lifecare var förskott på förmån – kontrollera vilket beslut som ska fattas"),
			tuple("EXPENSE_PARTIALLY_REJECTED", "RENT", "Ansökt belopp för Boendekostnad är 9000 kronor, 1000 kronor har inte godkänts – delavslag"),
			tuple("EXPENSE_PARTIALLY_REJECTED", "OTHER:Busskort", "Ansökt belopp för Övriga utgifter (Busskort) är 500 kronor, 500 kronor har inte godkänts – delavslag"));
	}

	@Test
	void avslagWhenTheEstimateIsNotPositive() {
		when(proposalBasisServiceMock.basis(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(basis(draft(), Optional.of(APPLICANT), Optional.of(new BigDecimal("1000"))));
		when(lifecareCaseHistoryServiceMock.listDecisions(APPLICANT, LocalDate.parse("2025-06-01"), LocalDate.parse("2026-06-30"))).thenReturn(List.of());
		when(warningServiceMock.reconcileByTypes(ERRAND_ID, DECISION_PROPOSAL_TYPES, List.of())).thenReturn(List.of());

		final var proposal = service.get(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(proposal.getEstimatedAmount()).isEqualByComparingTo("-950");
		assertThat(proposal.getOutcome()).isEqualTo("AVSLAG");
		assertThat(proposal.getPhraseText()).isNull();
		assertThat(proposal.getReason()).isNull();
		assertThat(proposal.getPreviousDecision()).isNull();
	}

	@Test
	void noNormMeansNoOutcomeButAnExplanation() {
		when(proposalBasisServiceMock.basis(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(basis(draft(), Optional.empty(), Optional.empty()));
		when(warningServiceMock.reconcileByTypes(ERRAND_ID, DECISION_PROPOSAL_TYPES, List.of())).thenReturn(List.of());

		final var proposal = service.get(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(proposal.getOutcome()).isNull();
		assertThat(proposal.getEstimatedAmount()).isNull();
		assertThat(proposal.getExplanation()).isEqualTo("Ingen norm kunde läsas från Lifecare – beloppet kunde inte beräknas.");
		verifyNoInteractions(lifecareCaseHistoryServiceMock);
	}

	@Test
	void aFailedLifecareDecisionReadIsBestEffort() {
		when(proposalBasisServiceMock.basis(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(basis(draft(), Optional.of(APPLICANT), Optional.of(new BigDecimal("6200"))));
		when(lifecareCaseHistoryServiceMock.listDecisions(APPLICANT, LocalDate.parse("2025-06-01"), LocalDate.parse("2026-06-30"))).thenThrow(Problem.valueOf(BAD_GATEWAY, "down"));
		when(warningServiceMock.reconcileByTypes(ERRAND_ID, DECISION_PROPOSAL_TYPES, List.of())).thenReturn(List.of());

		final var proposal = service.get(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(proposal.getOutcome()).isEqualTo("BIFALL");
		assertThat(proposal.getPreviousDecision()).isNull();
		verify(warningServiceMock).reconcileByTypes(ERRAND_ID, DECISION_PROPOSAL_TYPES, List.of());
	}

	@Test
	void theCoApplicantsOrsakIsProposedFromTheSamePreviousDecision() {
		when(proposalBasisServiceMock.basis(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(basis(draft(), Optional.of(APPLICANT), Optional.of(new BigDecimal("6200"))));
		when(lifecareCaseHistoryServiceMock.listDecisions(APPLICANT, LocalDate.parse("2025-06-01"), LocalDate.parse("2026-06-30")))
			.thenReturn(List.of(decision("Bifall", "Arbetslös, ingen ersättning/stöd", "Astrid Testsson", "Hemarbetande")));
		when(warningServiceMock.reconcileByTypes(ERRAND_ID, DECISION_PROPOSAL_TYPES, List.of())).thenReturn(List.of());

		final var proposal = service.get(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(proposal.getReason()).isEqualTo("Arbetslös, ingen ersättning/stöd"); // in the catalogue
		assertThat(proposal.getCoApplicantReason()).isEqualTo("Hemarbetande");
		assertThat(proposal.getReasonOptions()).containsExactlyElementsOf(withPreviousReason("Hemarbetande")); // only the co-applicant's is outside it
		assertThat(proposal.getPreviousDecision().getCoApplicant()).isEqualTo("Astrid Testsson");
		assertThat(proposal.getPreviousDecision().getCoApplicantReason()).isEqualTo("Hemarbetande");
	}

	@Test
	void aBlankCoApplicantReasonIsNoProposal() {
		when(proposalBasisServiceMock.basis(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(basis(draft(), Optional.of(APPLICANT), Optional.of(new BigDecimal("6200"))));
		when(lifecareCaseHistoryServiceMock.listDecisions(APPLICANT, LocalDate.parse("2025-06-01"), LocalDate.parse("2026-06-30")))
			.thenReturn(List.of(decision("Bifall", "Utan försörjningshinder", "Astrid Testsson", "  ")));
		when(warningServiceMock.reconcileByTypes(ERRAND_ID, DECISION_PROPOSAL_TYPES, List.of())).thenReturn(List.of());

		final var proposal = service.get(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(proposal.getCoApplicantReason()).isNull();
		assertThat(proposal.getReasonOptions()).containsExactlyElementsOf(DEFAULT_REASON_OPTIONS);
	}

	@Test
	void theOrsakCatalogueIsLifecaresOwnList() {
		assertThat(DEFAULT_REASON_OPTIONS)
			.hasSize(23)
			.doesNotHaveDuplicates()
			.startsWith("Arbetar deltid ofrivilligt, otillräcklig inkomst")
			.contains("Sjukskriven m läkarintyg, ingen sjukpenning", "Etableringsers. saknas (prestationsförmåga <25%)")
			.endsWith("Utan försörjningshinder")
			.doesNotContain("Arbetar deltid, ofrivilligt", "Arbetslös", "Sjukskriven med läkarintyg"); // dropdown group headings are not pickable
	}

	private static List<String> withPreviousReason(final String reason) {
		return Stream.concat(DEFAULT_REASON_OPTIONS.stream(), Stream.of(reason)).toList();
	}
}
