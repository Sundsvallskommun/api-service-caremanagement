package se.sundsvall.caremanagement.lifecare.integration.integrator;

import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationExpensePostDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationIncomePostDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationPersonPostDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationSpecialExpensePostDTO;
import generated.se.sundsvall.lifecarefamilycare.PostAktualiseringsBodyRequest;
import generated.se.sundsvall.lifecarefamilycare.PostCalculationBodyRequest;
import generated.se.sundsvall.lifecareintegrator.CalculationExpenseRequest;
import generated.se.sundsvall.lifecareintegrator.CalculationIncomeRequest;
import generated.se.sundsvall.lifecareintegrator.CalculationPersonRequest;
import generated.se.sundsvall.lifecareintegrator.CreateActualisationRequest;
import generated.se.sundsvall.lifecareintegrator.CreateCalculationRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

import static java.util.Optional.ofNullable;
import static se.sundsvall.caremanagement.lifecare.integration.integrator.IntegratorValues.mapEach;

/**
 * Translates the two FamilyCare write bodies careM assembles into the integrator's own requests.
 *
 * <p>
 * This is the read mapping run backwards, and the conversions go the other way with it: FamilyCare's rendered date
 * strings become {@link LocalDate}, its {@code Double} amounts become {@link BigDecimal}, and the
 * {@link OffsetDateTime} deviation bounds on a calculation person become plain dates. Nothing is rounded or
 * reinterpreted — a value careM did not set stays unset.
 *
 * <p>
 * The applicant is the one field that cannot be copied across. FamilyCare's body identifies the calculation owner by
 * personal identity number while the integrator wants a {@code partyId}, so the caller resolves it and passes it in.
 * The <em>household</em> persons are different: careM already holds party ids for them
 * ({@code EffectivePerson.partyId}), so those go straight through — see {@link #toCalculation} for why that makes this
 * route the more faithful of the two.
 */
final class IntegratorWriteMapper {

	private IntegratorWriteMapper() {}

	/**
	 * @param applicantPartyId the applicant, already resolved from the personal identity number in
	 *                         {@code body.personId}.
	 */
	static CreateCalculationRequest toCalculation(final PostCalculationBodyRequest body, final String applicantPartyId) {
		return new CreateCalculationRequest()
			.partyId(applicantPartyId)
			.serviceId(body.getServiceId())
			.investigationId(body.getInvestigationId())
			.normId(body.getNormId())
			.actualisationId(body.getAktualiseringId())
			.calculationDate(toDate(body.getCalculationDate()))
			.calculationFromDate(toDate(body.getCalculationFromDate()))
			.calculationToDate(toDate(body.getCalculationToDate()))
			.hasCustomHouseholdSize(body.getHasCustomHouseholdSize())
			.householdSize(body.getHouseholdSize())
			.persons(mapEach(body.getCalculationPersons(), IntegratorWriteMapper::toPerson))
			.incomes(mapEach(body.getCalculationIncomes(), IntegratorWriteMapper::toIncome))
			.expenses(mapEach(body.getCalculationExpenses(), IntegratorWriteMapper::toExpense))
			.specialExpenses(mapEach(body.getCalculationSpecialExpenses(), IntegratorWriteMapper::toSpecialExpense));
	}

	/**
	 * The household member's id is passed through unchanged, because careM already holds a {@code partyId} for it —
	 * {@code CalculationService.toPersonDto} fills FamilyCare's {@code personId} from
	 * {@code EffectivePerson.partyId()}. That is what the integrator wants, so nothing needs resolving here.
	 */
	private static CalculationPersonRequest toPerson(final PersonBasedCalculationPersonPostDTO person) {
		return new CalculationPersonRequest()
			.partyId(person.getPersonId())
			.numberOfDays(person.getNumberOfDays())
			.deviationFromDate(toDate(person.getDeviationFromDate()))
			.deviationToDate(toDate(person.getDeviationToDate()));
	}

	private static CalculationIncomeRequest toIncome(final PersonBasedCalculationIncomePostDTO income) {
		return new CalculationIncomeRequest()
			.typeId(income.getId())
			.applicantAmount(toAmount(income.getApplicantAmount()))
			.applicantAmountDate(toDate(income.getApplicantAmountDate()))
			.coApplicantAmount(toAmount(income.getCoApplicantAmount()))
			.coApplicantAmountDate(toDate(income.getCoApplicantAmountDate()))
			.note(income.getNote());
	}

	private static CalculationExpenseRequest toExpense(final PersonBasedCalculationExpensePostDTO expense) {
		return new CalculationExpenseRequest()
			.typeId(expense.getId())
			.amount(toAmount(expense.getAmount()))
			.approvedAmount(toAmount(expense.getApprovedAmount()))
			.note(expense.getNote());
	}

	private static CalculationExpenseRequest toSpecialExpense(final PersonBasedCalculationSpecialExpensePostDTO expense) {
		return new CalculationExpenseRequest()
			.typeId(expense.getId())
			.amount(toAmount(expense.getAmount()))
			.approvedAmount(toAmount(expense.getApprovedAmount()))
			.note(expense.getNote());
	}

	/**
	 * @param applicantPartyId the applicant, already resolved from the personal identity number in
	 *                         {@code body.personId}.
	 */
	static CreateActualisationRequest toActualisation(final PostAktualiseringsBodyRequest body, final String applicantPartyId) {
		return new CreateActualisationRequest()
			.partyId(applicantPartyId)
			.date(toDate(body.getDate()))
			.typeId(body.getType())
			.fromWhoId(body.getFromWho())
			.reasonId(body.getReason())
			.organisationId(body.getOrganisationId())
			.organisationUnitId(body.getOrganisationUnitId())
			.caseworkerId(body.getCaseworkerId())
			.specifiesId(body.getSpecifies())
			.serviceId(body.getServiceId())
			.investigationId(body.getInvestigationId())
			.workingStatusId(body.getWorkingStatus());
	}

	// ---- Conversions -------------------------------------------------------------------------------------------------

	/**
	 * FamilyCare's own rendering is {@code yyyy-MM-dd'T'HH:mm:ss} (see {@code FamilyCareDates}), so the leading ten
	 * characters are the date. Taking them rather than parsing the whole string keeps a bare {@code yyyy-MM-dd} and any
	 * other suffix working; anything that is not a date in front becomes {@code null}, which the caller's
	 * required-field check then reports by name instead of the integrator rejecting an empty body.
	 */
	private static LocalDate toDate(final String rendered) {
		return ofNullable(rendered)
			.filter(value -> value.length() >= 10)
			.map(value -> {
				try {
					return LocalDate.parse(value.substring(0, 10));
				} catch (final DateTimeParseException e) {
					return null;
				}
			})
			.orElse(null);
	}

	private static LocalDate toDate(final OffsetDateTime timestamp) {
		return ofNullable(timestamp).map(OffsetDateTime::toLocalDate).orElse(null);
	}

	private static BigDecimal toAmount(final Double value) {
		return ofNullable(value).map(BigDecimal::valueOf).orElse(null);
	}
}
