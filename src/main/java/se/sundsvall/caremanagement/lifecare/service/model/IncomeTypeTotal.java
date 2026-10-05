package se.sundsvall.caremanagement.lifecare.service.model;

import java.math.BigDecimal;
import java.util.List;
import se.sundsvall.caremanagement.lifecare.service.mapper.MapperUtil;

/**
 * What this month's SSBTEK transfer puts on one FamilyCare income type, the whole household together: the applicant's,
 * the co-applicant's and the household children's incomes (a child's income is folded into the applicant's column)
 * summed into one amount. The unit the previous-normberäkning income comparison works in, since the normberäkning
 * itself only knows income types, not SSBTEK benefits.
 *
 * @param typeName the FamilyCare income-type name, as the calculation proposal spells it
 * @param amount   the summed net amount of the incomes resolved to the type
 * @param benefits the distinct SSBTEK benefit names ({@code forman}) that fed the type, sorted
 */
public record IncomeTypeTotal(
	String typeName,
	BigDecimal amount,
	List<String> benefits) {

	/**
	 * The type name normalised the way the previous normberäkning's amounts are keyed
	 * ({@code LifecareCaseService.previousCalculationIncomeTypeAmounts}), so the two sides meet on the same key.
	 *
	 * @return the trimmed, lower-cased type name, empty when there is none
	 */
	public String key() {
		return MapperUtil.normalize(typeName);
	}
}
