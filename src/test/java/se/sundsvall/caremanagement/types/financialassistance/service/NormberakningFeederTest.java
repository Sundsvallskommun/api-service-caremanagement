package se.sundsvall.caremanagement.types.financialassistance.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.lifecare.service.model.FcIncomeLine;
import se.sundsvall.caremanagement.lifecare.service.model.PreviousHousehold;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaChild;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaCost;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormPersonEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaPerson;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static se.sundsvall.caremanagement.types.financialassistance.service.NormberakningConstants.ORIGIN_SYSTEM;
import static se.sundsvall.caremanagement.types.financialassistance.service.NormberakningConstants.ROLE_CHILD;

@ExtendWith(MockitoExtension.class)
class NormberakningFeederTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = "errand-1";

	@Mock
	private ExpenseRegelverkService expenseRegelverkServiceMock;

	@InjectMocks
	private NormberakningFeeder feeder;

	@Test
	void incomeRowsMapsEachLine() {
		final var date = OffsetDateTime.parse("2026-05-15T00:00:00Z");
		final var lines = List.of(
			new FcIncomeLine(20, "Bostadsbidrag", "APPLICANT", new BigDecimal("1850"), date, "note"),
			new FcIncomeLine(21, "Lön", "CO_APPLICANT", new BigDecimal("12000"), date, null));

		final var rows = feeder.incomeRows(ERRAND_ID, lines);

		assertThat(rows).hasSize(2);
		assertThat(rows).allMatch(r -> ERRAND_ID.equals(r.getErrandId()) && ORIGIN_SYSTEM.equals(r.getOrigin()));
		final var first = rows.getFirst();
		assertThat(first.getTypeId()).isEqualTo(20);
		assertThat(first.getTypeName()).isEqualTo("Bostadsbidrag");
		assertThat(first.getApplicantProcessAmount()).isEqualByComparingTo(new BigDecimal("1850"));
		assertThat(first.getApplicantAmountDate()).isEqualTo(date);
	}

	@Test
	void incomeRowsHandlesNullLines() {
		assertThat(feeder.incomeRows(ERRAND_ID, null)).isEmpty();
	}

	@Test
	void expenseRowsCapsEachCost() {
		final var rent = FaCost.create().withCostType("RENT").withOtherSubType(null).withSpecification("spec")
			.withAppliedAmount(new BigDecimal("9000"));
		final var errand = FinancialAssistanceEntity.create()
			.withHousingForm("RENTAL").withHousingPersonCount(2).withNormType("RIKSNORM")
			.withCosts(List.of(rent));

		when(expenseRegelverkServiceMock.verdict(eq(MUNICIPALITY_ID), eq("RENT"), any(), eq("RENTAL"), eq(2), eq("RIKSNORM"), eq(new BigDecimal("9000"))))
			.thenReturn(new ExpenseRegelverkService.ExpenseVerdict(new BigDecimal("8500"), "SPECIAL_EXPENSE"));

		final var rows = feeder.expenseRows(MUNICIPALITY_ID, ERRAND_ID, errand);

		assertThat(rows).hasSize(1);
		final var row = rows.getFirst();
		assertThat(row.getErrandId()).isEqualTo(ERRAND_ID);
		assertThat(row.getOrigin()).isEqualTo(ORIGIN_SYSTEM);
		assertThat(row.getCostType()).isEqualTo("RENT");
		assertThat(row.getSpecification()).isEqualTo("spec");
		assertThat(row.getAppliedAmount()).isEqualByComparingTo(new BigDecimal("9000"));
		assertThat(row.getProcessAmount()).isEqualByComparingTo(new BigDecimal("8500"));
		assertThat(row.getBucket()).isEqualTo("SPECIAL_EXPENSE");
	}

	@Test
	void expenseRowsHandlesNullCosts() {
		final var errand = FinancialAssistanceEntity.create();

		assertThat(feeder.expenseRows(MUNICIPALITY_ID, ERRAND_ID, errand)).isEmpty();
	}

	@Test
	void personRowsMapsPersonsAndChildren() {
		final var applicant = FaPerson.create().withRole("APPLICANT").withPartyId("p-1");
		final var child = FaChild.create().withPartyId("c-1").withFirstName("Anna").withLastName("Svensson").withDaysInHome(15);
		final var childNoDays = FaChild.create().withPartyId("c-2").withFirstName("Bo").withLastName(null).withDaysInHome(null);
		final var errand = FinancialAssistanceEntity.create()
			.withPersons(List.of(applicant))
			.withChildren(List.of(child, childNoDays));

		final var rows = feeder.personRows(ERRAND_ID, errand);

		assertThat(rows).hasSize(3);
		assertThat(rows).allMatch(r -> ERRAND_ID.equals(r.getErrandId()) && ORIGIN_SYSTEM.equals(r.getOrigin()));

		final var personRow = rows.getFirst();
		assertThat(personRow.getPartyId()).isEqualTo("p-1");
		assertThat(personRow.getRole()).isEqualTo("APPLICANT");
		assertThat(personRow.getProcessDays()).isEqualTo(30);

		final var childRow = rows.get(1);
		assertThat(childRow.getPartyId()).isEqualTo("c-1");
		assertThat(childRow.getRole()).isEqualTo(ROLE_CHILD);
		assertThat(childRow.getName()).isEqualTo("Anna Svensson");
		assertThat(childRow.getProcessDays()).isEqualTo(15);

		final var childNoDaysRow = rows.get(2);
		assertThat(childNoDaysRow.getName()).isEqualTo("Bo");
		assertThat(childNoDaysRow.getProcessDays()).isEqualTo(30);
	}

	@Test
	void personRowsHandlesNullCollections() {
		assertThat(feeder.personRows(ERRAND_ID, FinancialAssistanceEntity.create())).isEmpty();
	}

	@Test
	void householdWarningsReturnsEmptyForEmptyPrevious() {
		final var current = List.of(FaNormPersonEntity.create().withPartyId("p-1"));

		assertThat(feeder.householdWarnings(current, PreviousHousehold.empty())).isEmpty();
		assertThat(feeder.householdWarnings(current, null)).isEmpty();
	}

	@Test
	void householdWarningsFlagsMissingMember() {
		final var current = List.of(FaNormPersonEntity.create().withPartyId("p-1"));
		final var previous = new PreviousHousehold(Set.of("p-1", "p-2"), 1, null);

		final var warnings = feeder.householdWarnings(current, previous);

		assertThat(warnings).contains("Hushållsmedlem från föregående normberäkning saknas nu: p-2");
	}

	@Test
	void householdWarningsFlagsCountChange() {
		final var current = List.of(
			FaNormPersonEntity.create().withPartyId("p-1"),
			FaNormPersonEntity.create().withPartyId("p-2"));
		final var previous = new PreviousHousehold(Set.of("p-1", "p-2"), 3, null);

		final var warnings = feeder.householdWarnings(current, previous);

		assertThat(warnings).contains("Antal hushållsmedlemmar har ändrats sedan föregående normberäkning (föregående 3, nu 2)");
	}
}
