package se.sundsvall.caremanagement.types.financialassistance.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.lifecare.service.LifecareCaseHistoryService;
import se.sundsvall.caremanagement.lifecare.service.model.PaymentView;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CalculationDraft;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Payee;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Warning;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaPerson;
import se.sundsvall.dept44.problem.Problem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static se.sundsvall.caremanagement.types.financialassistance.service.WarningService.PAYMENT_PROPOSAL_TYPES;

@ExtendWith(MockitoExtension.class)
class PaymentProposalServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = "errand-1";
	private static final String APPLICANT = "pnr-a";
	private static final YearMonth MONTH = YearMonth.parse("2026-06");
	private static final LocalDate FROM = LocalDate.parse("2025-06-01");
	private static final LocalDate TO = LocalDate.parse("2026-06-30");
	private static final List<WarningService.WarningInput> CO_APPLICANT_WARNING = List.of(new WarningService.WarningInput("CO_APPLICANT_SPLIT_PAYMENT", "co-applicant",
		"Det finns medsökande i ärendet – kontrollera om det ska vara delad utbetalning"));

	@Mock
	private ProposalBasisService proposalBasisServiceMock;

	@Mock
	private LifecareCaseHistoryService lifecareCaseHistoryServiceMock;

	@Mock
	private WarningService warningServiceMock;

	@InjectMocks
	private PaymentProposalService service;

	private static ProposalBasisService.ProposalBasis basis(final Optional<String> applicant, final boolean coApplicant, final Optional<FaPerson> person, final Optional<BigDecimal> amount) {
		final var household = new HouseholdPartyService.Household(applicant, coApplicant, person, Optional.of("Anna Andersson"));
		return new ProposalBasisService.ProposalBasis(CalculationDraft.create().withApplicationMonth("2026-06"), household, Optional.of(MONTH), amount, amount);
	}

	private static PaymentView payment(final String payDate, final String name, final String account) {
		return new PaymentView(1, new BigDecimal("8500"), "Bankkonto", payDate, "1234", account, name, null, null, null, null, "EB", "2026-05");
	}

	@Test
	void proposalFromThePreviousPaymentWithCoApplicantWarning() {
		when(proposalBasisServiceMock.basis(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(basis(Optional.of(APPLICANT), true, Optional.of(FaPerson.create().withPaymentSameAsPrevious(true)), Optional.of(new BigDecimal("4250"))));
		when(lifecareCaseHistoryServiceMock.listPayments(APPLICANT, FROM, TO)).thenReturn(List.of(
			payment("2026-04-27", "Anna Andersson", "111"),
			payment("2026-05-27", "Anna Andersson", "222"), // the latest by pay date → previous payment
			payment(null, "Bo", "333"))); // never paid → not the previous payment, still a payee option
		final var reconciled = List.of(Warning.create().withType("CO_APPLICANT_SPLIT_PAYMENT"));
		when(warningServiceMock.reconcileByTypes(ERRAND_ID, PAYMENT_PROPOSAL_TYPES, CO_APPLICANT_WARNING)).thenReturn(reconciled);

		final var proposal = service.get(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(proposal.getPayments()).singleElement().satisfies(payment -> {
			assertThat(payment.getPaymentDate()).isEqualTo(LocalDate.parse("2026-06-26")); // the 27th is a Saturday
			assertThat(payment.getAmount()).isEqualByComparingTo("4250");
			assertThat(payment.getConcernedMonth()).isEqualTo("2026-06");
			assertThat(payment.getPayee()).isEqualTo(Payee.create().withName("Anna Andersson").withPaymentMethod("Bankkonto").withClearing("1234").withAccountNumber("222"));
			assertThat(payment.getAccountingCode()).isNull();
		});
		assertThat(proposal.getPayeeSource()).isEqualTo("PREVIOUS_PAYMENT");
		assertThat(proposal.getPayeeOptions()).extracting(Payee::getAccountNumber).containsExactly("111", "222", "333");
		assertThat(proposal.getPreviousPayment().getPayDate()).isEqualTo("2026-05-27");
		assertThat(proposal.getPreviousPayment().getAccountNumber()).isEqualTo("222");
		assertThat(proposal.getExplanation()).isNull();
		assertThat(proposal.getWarnings()).isSameAs(reconciled);
	}

	@Test
	void applicationAccountWinsWhenNotSameAsPrevious() {
		final var person = FaPerson.create().withPaymentSameAsPrevious(false).withPaymentMethod("Bankkonto").withClearingNumber("9999").withAccountNumber("new-1");
		when(proposalBasisServiceMock.basis(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(basis(Optional.of(APPLICANT), false, Optional.of(person), Optional.of(new BigDecimal("4250"))));
		when(lifecareCaseHistoryServiceMock.listPayments(APPLICANT, FROM, TO)).thenReturn(List.of(payment("2026-05-27", "Anna Andersson", "222")));
		when(warningServiceMock.reconcileByTypes(ERRAND_ID, PAYMENT_PROPOSAL_TYPES, List.of())).thenReturn(List.of());

		final var proposal = service.get(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(proposal.getPayeeSource()).isEqualTo("APPLICATION");
		assertThat(proposal.getPayments().getFirst().getPayee()).isEqualTo(Payee.create().withName("Anna Andersson").withPaymentMethod("Bankkonto").withClearing("9999").withAccountNumber("new-1"));
		assertThat(proposal.getPreviousPayment()).isNotNull();
		assertThat(proposal.getWarnings()).isEmpty();
	}

	@Test
	void noPayeeWhenNothingIsKnownAndNoNormMeansAnExplanation() {
		when(proposalBasisServiceMock.basis(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(basis(Optional.empty(), false, Optional.empty(), Optional.empty()));
		when(warningServiceMock.reconcileByTypes(ERRAND_ID, PAYMENT_PROPOSAL_TYPES, List.of())).thenReturn(List.of());

		final var proposal = service.get(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(proposal.getPayments()).singleElement().satisfies(payment -> {
			assertThat(payment.getPaymentDate()).isEqualTo(LocalDate.parse("2026-06-26"));
			assertThat(payment.getAmount()).isNull();
			assertThat(payment.getPayee()).isNull();
		});
		assertThat(proposal.getPayeeSource()).isNull();
		assertThat(proposal.getPayeeOptions()).isEmpty();
		assertThat(proposal.getPreviousPayment()).isNull();
		assertThat(proposal.getExplanation()).isEqualTo("Ingen norm kunde läsas från Lifecare – beloppet kunde inte beräknas.");
		verifyNoInteractions(lifecareCaseHistoryServiceMock);
	}

	@Test
	void aFailedLifecarePaymentReadIsBestEffort() {
		when(proposalBasisServiceMock.basis(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(basis(Optional.of(APPLICANT), false, Optional.empty(), Optional.of(new BigDecimal("4250"))));
		when(lifecareCaseHistoryServiceMock.listPayments(APPLICANT, FROM, TO)).thenThrow(Problem.valueOf(BAD_GATEWAY, "down"));
		when(warningServiceMock.reconcileByTypes(ERRAND_ID, PAYMENT_PROPOSAL_TYPES, List.of())).thenReturn(List.of());

		final var proposal = service.get(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(proposal.getPayments().getFirst().getAmount()).isEqualByComparingTo("4250");
		assertThat(proposal.getPayeeSource()).isNull();
		assertThat(proposal.getPreviousPayment()).isNull();
	}
}
