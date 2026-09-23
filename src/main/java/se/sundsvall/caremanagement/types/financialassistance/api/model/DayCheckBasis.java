package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

/**
 * The SSBTEK facts that gate the dagersättning day check (verksamhetens svar 2026-09-23 §2): does Arbetsförmedlingen
 * report an ekonomiskt beslut, and has the applicant used up all 450 days of the jobb- och utvecklingsgaranti according
 * to Försäkringskassan. Neither is an income, so neither travels in {@code classifiedIncomes}.
 *
 * <p>
 * Every field is tri-state on purpose: {@code null} means "the caller did not read this", which is not the same as "the
 * agency answered no". An unread gate never produces a warning — the check is simply not made.
 * </p>
 */
@Schema(description = """
	The SSBTEK facts that gate the dagersättning day check: Arbetsförmedlingen's economic-decision periods and \
	Försäkringskassan's consumed jobb- och utvecklingsgaranti days. A null field means the caller did not read it, and \
	the day check is then not made at all.""")
public class DayCheckBasis {

	@Schema(description = """
		Arbetsförmedlingen's economic-decision periods (af BeslutInfo.EkonomiskaBeslut.Beslut). An empty list means AF \
		answered and reports no decision; null means AF was not read or could not answer.""")
	private List<EconomicDecisionPeriod> economicDecisionPeriods;

	@Schema(description = """
		Försäkringskassan's consumed days in the jobb- och utvecklingsgaranti (fk formansinformation.programjobdagar \
		antalForbrukade, 'Förbrukade dagar'); null when not read""", examples = "212")
	private Integer consumedDays;

	@Schema(description = """
		Whether Försäkringskassan reports all days used up (fk formansinformation.programjobdagar harForbrukatMaxAntal, \
		'Alla dagar förbrukade'). False when FK answered without any programjobdagar; null when FK was not read.""", examples = "false")
	private Boolean allDaysConsumed;

	public static DayCheckBasis create() {
		return new DayCheckBasis();
	}

	public List<EconomicDecisionPeriod> getEconomicDecisionPeriods() {
		return economicDecisionPeriods;
	}

	public void setEconomicDecisionPeriods(final List<EconomicDecisionPeriod> economicDecisionPeriods) {
		this.economicDecisionPeriods = economicDecisionPeriods;
	}

	public DayCheckBasis withEconomicDecisionPeriods(final List<EconomicDecisionPeriod> economicDecisionPeriods) {
		this.economicDecisionPeriods = economicDecisionPeriods;
		return this;
	}

	public Integer getConsumedDays() {
		return consumedDays;
	}

	public void setConsumedDays(final Integer consumedDays) {
		this.consumedDays = consumedDays;
	}

	public DayCheckBasis withConsumedDays(final Integer consumedDays) {
		this.consumedDays = consumedDays;
		return this;
	}

	public Boolean getAllDaysConsumed() {
		return allDaysConsumed;
	}

	public void setAllDaysConsumed(final Boolean allDaysConsumed) {
		this.allDaysConsumed = allDaysConsumed;
	}

	public DayCheckBasis withAllDaysConsumed(final Boolean allDaysConsumed) {
		this.allDaysConsumed = allDaysConsumed;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final DayCheckBasis that = (DayCheckBasis) o;
		return Objects.equals(economicDecisionPeriods, that.economicDecisionPeriods) && Objects.equals(consumedDays, that.consumedDays)
			&& Objects.equals(allDaysConsumed, that.allDaysConsumed);
	}

	@Override
	public int hashCode() {
		return Objects.hash(economicDecisionPeriods, consumedDays, allDaysConsumed);
	}

	@Override
	public String toString() {
		return "DayCheckBasis{" +
			"economicDecisionPeriods=" + economicDecisionPeriods +
			", consumedDays=" + consumedDays +
			", allDaysConsumed=" + allDaysConsumed +
			'}';
	}
}
