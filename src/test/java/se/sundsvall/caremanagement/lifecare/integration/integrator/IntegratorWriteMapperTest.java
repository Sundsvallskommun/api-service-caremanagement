package se.sundsvall.caremanagement.lifecare.integration.integrator;

import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationExpensePostDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationIncomePostDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationPersonPostDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationSpecialExpensePostDTO;
import generated.se.sundsvall.lifecarefamilycare.PostAktualiseringsBodyRequest;
import generated.se.sundsvall.lifecarefamilycare.PostCalculationBodyRequest;
import generated.se.sundsvall.lifecareintegrator.CalculationExpenseRequest;
import generated.se.sundsvall.lifecareintegrator.CalculationIncomeRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static java.time.Month.JUNE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class IntegratorWriteMapperTest {

	private static final String APPLICANT_PARTY_ID = "6a5c3d18-1f2b-4e77-9c0a-2b3d4e5f6a7b";
	private static final String MEMBER_PARTY_ID = "1a2b3c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d";

	@Test
	void theCalculationBodyIsMapped() {
		final var body = new PostCalculationBodyRequest()
			.personId("200001012384")
			.serviceId(42)
			.investigationId(11)
			.normId(7)
			.aktualiseringId(88)
			.calculationDate("2026-06-01T00:00:00")
			.calculationFromDate("2026-06-01T00:00:00")
			.calculationToDate("2026-06-30T00:00:00")
			.hasCustomHouseholdSize(true)
			.householdSize(3)
			.calculationPersons(List.of(new PersonBasedCalculationPersonPostDTO()
				.personId(MEMBER_PARTY_ID)
				.numberOfDays(30)
				.deviationFromDate(OffsetDateTime.of(2026, 6, 10, 14, 30, 0, 0, ZoneOffset.UTC))))
			.calculationIncomes(List.of(new PersonBasedCalculationIncomePostDTO()
				.id(1)
				.applicantAmount(8000.0)
				.applicantAmountDate(OffsetDateTime.of(2026, 6, 25, 0, 0, 0, 0, ZoneOffset.UTC))
				.coApplicantAmount(1500.5)
				.note("Lön juni")))
			.calculationExpenses(List.of(new PersonBasedCalculationExpensePostDTO().id(2).amount(9000.0).approvedAmount(6500.0).note("Hyra")))
			.calculationSpecialExpenses(List.of(new PersonBasedCalculationSpecialExpensePostDTO().id(3).amount(450.0).approvedAmount(450.0)));

		final var request = IntegratorWriteMapper.toCalculation(body, APPLICANT_PARTY_ID);

		assertThat(request.getPartyId()).isEqualTo(APPLICANT_PARTY_ID);
		assertThat(request.getServiceId()).isEqualTo(42);
		assertThat(request.getInvestigationId()).isEqualTo(11);
		assertThat(request.getNormId()).isEqualTo(7);
		assertThat(request.getActualisationId()).isEqualTo(88);
		assertThat(request.getCalculationDate()).isEqualTo(LocalDate.of(2026, JUNE, 1));
		assertThat(request.getCalculationFromDate()).isEqualTo(LocalDate.of(2026, JUNE, 1));
		assertThat(request.getCalculationToDate()).isEqualTo(LocalDate.of(2026, JUNE, 30));
		assertThat(request.getHasCustomHouseholdSize()).isTrue();
		assertThat(request.getHouseholdSize()).isEqualTo(3);

		assertThat(request.getPersons()).singleElement().satisfies(person -> {
			assertThat(person.getPartyId()).isEqualTo(MEMBER_PARTY_ID);
			assertThat(person.getNumberOfDays()).isEqualTo(30);
			assertThat(person.getDeviationFromDate()).isEqualTo(LocalDate.of(2026, JUNE, 10));
			assertThat(person.getDeviationToDate()).isNull();
		});
		assertThat(request.getIncomes()).singleElement().satisfies(income -> {
			assertThat(income.getTypeId()).isEqualTo(1);
			assertThat(income.getApplicantAmount()).isEqualByComparingTo("8000.0");
			assertThat(income.getApplicantAmountDate()).isEqualTo(LocalDate.of(2026, JUNE, 25));
			assertThat(income.getCoApplicantAmount()).isEqualByComparingTo("1500.5");
			assertThat(income.getCoApplicantAmountDate()).isNull();
			assertThat(income.getNote()).isEqualTo("Lön juni");
		});
		assertThat(request.getExpenses())
			.extracting(CalculationExpenseRequest::getTypeId, CalculationExpenseRequest::getNote)
			.containsExactly(tuple(2, "Hyra"));
		assertThat(request.getExpenses()).singleElement().satisfies(expense -> {
			assertThat(expense.getAmount()).isEqualByComparingTo("9000.0");
			assertThat(expense.getApprovedAmount()).isEqualByComparingTo("6500.0");
		});
		assertThat(request.getSpecialExpenses()).singleElement().satisfies(expense -> {
			assertThat(expense.getTypeId()).isEqualTo(3);
			assertThat(expense.getApprovedAmount()).isEqualByComparingTo("450.0");
		});
	}

	/**
	 * The household member's id is passed through, not resolved: careM already holds a partyId for it. Resolving it
	 * again would fail, and dropping it would post a calculation with no norm base.
	 */
	@Test
	void householdMemberIdsArePassedThroughUnchanged() {
		final var body = new PostCalculationBodyRequest()
			.calculationPersons(List.of(
				new PersonBasedCalculationPersonPostDTO().personId(MEMBER_PARTY_ID),
				new PersonBasedCalculationPersonPostDTO().personId(APPLICANT_PARTY_ID)));

		assertThat(IntegratorWriteMapper.toCalculation(body, APPLICANT_PARTY_ID).getPersons())
			.extracting(person -> person.getPartyId())
			.containsExactly(MEMBER_PARTY_ID, APPLICANT_PARTY_ID);
	}

	@Test
	void anEmptyCalculationBodyMapsToEmptyListsNotNulls() {
		final var request = IntegratorWriteMapper.toCalculation(new PostCalculationBodyRequest(), APPLICANT_PARTY_ID);

		assertThat(request.getPersons()).isEmpty();
		assertThat(request.getIncomes()).isEmpty();
		assertThat(request.getExpenses()).isEmpty();
		assertThat(request.getSpecialExpenses()).isEmpty();
		assertThat(request.getNormId()).isNull();
		assertThat(request.getCalculationDate()).isNull();
	}

	/** FamilyCare renders {@code yyyy-MM-dd'T'HH:mm:ss}; a bare date has to keep working too. */
	@ParameterizedTest
	@ValueSource(strings = {
		"2026-06-01T00:00:00", "2026-06-01", "2026-06-01T23:59:59", "2026-06-01T00:00:00+02:00"
	})
	void bothRenderedAndBareDatesParse(final String rendered) {
		final var body = new PostCalculationBodyRequest().calculationDate(rendered);

		assertThat(IntegratorWriteMapper.toCalculation(body, APPLICANT_PARTY_ID).getCalculationDate())
			.isEqualTo(LocalDate.of(2026, JUNE, 1));
	}

	/**
	 * A date that will not parse becomes null rather than an exception, so the caller's required-field check reports
	 * it by name instead of the failure surfacing as a stack trace from inside the mapper.
	 */
	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {
		"not-a-date", "2026-13-45T00:00:00", "26-06-01"
	})
	void anUnparseableDateBecomesNull(final String rendered) {
		final var body = new PostCalculationBodyRequest().calculationDate(rendered);

		assertThat(IntegratorWriteMapper.toCalculation(body, APPLICANT_PARTY_ID).getCalculationDate()).isNull();
	}

	@Test
	void theActualisationBodyIsMapped() {
		final var body = new PostAktualiseringsBodyRequest()
			.personId("200001012384")
			.date("2026-06-12T00:00:00")
			.type(1)
			.fromWho(20)
			.reason(10)
			.organisationId(4)
			.organisationUnitId("IFO-EB")
			.caseworkerId("kaka01")
			.specifies(2)
			.serviceId(42)
			.investigationId(11)
			.workingStatus(3);

		final var request = IntegratorWriteMapper.toActualisation(body, APPLICANT_PARTY_ID);

		assertThat(request.getPartyId()).isEqualTo(APPLICANT_PARTY_ID);
		assertThat(request.getDate()).isEqualTo(LocalDate.of(2026, JUNE, 12));
		assertThat(request.getTypeId()).isEqualTo(1);
		assertThat(request.getFromWhoId()).isEqualTo(20);
		assertThat(request.getReasonId()).isEqualTo(10);
		assertThat(request.getOrganisationId()).isEqualTo(4);
		assertThat(request.getOrganisationUnitId()).isEqualTo("IFO-EB");
		assertThat(request.getCaseworkerId()).isEqualTo("kaka01");
		assertThat(request.getSpecifiesId()).isEqualTo(2);
		assertThat(request.getServiceId()).isEqualTo(42);
		assertThat(request.getInvestigationId()).isEqualTo(11);
		assertThat(request.getWorkingStatusId()).isEqualTo(3);
	}

	/**
	 * FamilyCare's field names lose the {@code Id} suffix the integrator uses ({@code type} vs {@code typeId},
	 * {@code specifies} vs {@code specifiesId}), so the pairing is worth pinning: a mismatched pair would post a valid
	 * actualisation with the wrong reason or working status on it.
	 */
	@Test
	void theActualisationIdFieldsAreNotTransposed() {
		final var request = IntegratorWriteMapper.toActualisation(
			new PostAktualiseringsBodyRequest().type(1).fromWho(2).reason(3).specifies(4).workingStatus(5), APPLICANT_PARTY_ID);

		assertThat(request.getTypeId()).isEqualTo(1);
		assertThat(request.getFromWhoId()).isEqualTo(2);
		assertThat(request.getReasonId()).isEqualTo(3);
		assertThat(request.getSpecifiesId()).isEqualTo(4);
		assertThat(request.getWorkingStatusId()).isEqualTo(5);
	}

	/** An income row with nothing but its type still maps — a zero-amount row is a real thing in a normberäkning. */
	@Test
	void amountsThatAreUnsetStayUnset() {
		final var body = new PostCalculationBodyRequest()
			.calculationIncomes(List.of(new PersonBasedCalculationIncomePostDTO().id(1)));

		assertThat(IntegratorWriteMapper.toCalculation(body, APPLICANT_PARTY_ID).getIncomes())
			.extracting(CalculationIncomeRequest::getTypeId, CalculationIncomeRequest::getApplicantAmount, CalculationIncomeRequest::getCoApplicantAmount)
			.containsExactly(tuple(1, null, null));
	}

	@Test
	void amountsKeepTheirScale() {
		final var body = new PostCalculationBodyRequest()
			.calculationExpenses(List.of(new PersonBasedCalculationExpensePostDTO().id(1).amount(6512.75)));

		assertThat(IntegratorWriteMapper.toCalculation(body, APPLICANT_PARTY_ID).getExpenses())
			.singleElement()
			.satisfies(expense -> assertThat(expense.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(6512.75)));
	}
}
