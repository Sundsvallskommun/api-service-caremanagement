package se.sundsvall.caremanagement.lifecare.service.mapper;

import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationProposalDTO;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import se.sundsvall.caremanagement.lifecare.service.model.ApplicationIncome;
import se.sundsvall.caremanagement.lifecare.service.model.FamilyCareIncomeLine;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

/**
 * Translates the incomes a citizen declared in a financial-assistance application into the same
 * {@link FamilyCareIncomeLine}
 * the SSBTEK path produces ({@link ClassifiedIncomeToFamilyCareMapper#toIncomeLines}) — so everything downstream (the
 * {@code CalculationFeeder} fold into one row per FamilyCare type, the draft/effective conversion, the commit to
 * Lifecare) is
 * shared, not rebuilt. The only application-specific step lives here: map the application's own income code to the
 * FamilyCare
 * income-type name and resolve that name to the numeric id the calculation proposal offers. One line per declared
 * income; incomes whose type does not resolve to an FamilyCare id are skipped.
 */
public final class ApplicationIncomeToFamilyCareMapper {

	private ApplicationIncomeToFamilyCareMapper() {}

	private static final String FC_OTHER_INCOME = "Övriga inkomster";

	/**
	 * The application income code → FamilyCare income-type name table. The values must match the names Lifecare returns in
	 * the
	 * calculation proposal ({@code calculationIncomeTypes}); matching is case-insensitive and trim-insensitive. Edit here
	 * when the FamilyCare catalogue or the application's income codes change.
	 */
	static final Map<String, String> APPLICATION_TYPE_TO_FC_NAME = Map.of(
		"SALARY", "Lön efter skatt",
		"SWISH_DEPOSITS", "Swish/Insättningar/Överföringar",
		"OCCUPATIONAL_PENSION_INSURANCE", "Pension/SA/Livränta/Omvårdnadsbidrag",
		"CHILD_SUPPORT", "Underhållsstöd",
		"RENT_SHARE_FROM_CHILD", FC_OTHER_INCOME,
		"OTHER_INCOME", FC_OTHER_INCOME,
		"FINANCIAL_AID_OTHER_MUNICIPALITY", FC_OTHER_INCOME);

	/**
	 * Map the declared application incomes to FamilyCare income lines for the given calculation proposal — one line per
	 * (resolved
	 * FamilyCare type, recipient), in the same shape {@link ClassifiedIncomeToFamilyCareMapper#toIncomeLines} returns so
	 * the existing
	 * fold + commit pipeline consumes them unchanged.
	 *
	 * @param  incomes  the incomes declared in the application (maybe {@code null})
	 * @param  proposal the FamilyCare calculation proposal whose {@code calculationIncomeTypes} supply the numeric type ids
	 * @return          one income line per resolved declared income (unresolved types skipped)
	 */
	public static List<FamilyCareIncomeLine> toIncomeLines(final List<ApplicationIncome> incomes, final PersonBasedCalculationProposalDTO proposal) {
		final var typeIdByName = MapperUtil.indexIncomeTypeIds(proposal);

		return ofNullable(incomes).orElse(emptyList()).stream()
			.filter(Objects::nonNull)
			.map(income -> toLine(income, typeIdByName))
			.filter(Objects::nonNull)
			.toList();
	}

	private static FamilyCareIncomeLine toLine(final ApplicationIncome income, final Map<String, Integer> typeIdByName) {
		final var fcName = APPLICATION_TYPE_TO_FC_NAME.get(ofNullable(income.incomeType()).orElse(""));
		if (fcName == null) {
			return null;
		}
		final var typeId = typeIdByName.get(MapperUtil.normalize(fcName));
		if (typeId == null) {
			return null;
		}
		if (income.role() == null) {
			// Without a recipient the line can't be posted to FamilyCare — skip it rather than NPE on role().name().
			return null;
		}
		return new FamilyCareIncomeLine(typeId, fcName, income.role().name(), income.amount(), MapperUtil.toOffsetDateTime(income.date()), "Ansökan");
	}
}
