package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Result of building and posting an SSBTEK-driven normberäkning: the id of the calculation created in Lifecare FC, plus
 * the warnings the handläggare must review — incomes Drakel could not auto-transfer ({@code unhandledIncomes}) and
 * förmåner whose net income changed beyond the threshold between the rule periods ({@code changeWarnings}). The
 * warnings
 * are human-readable lines; the structured form lives in the service layer.
 */
@Schema(description = "The created Lifecare normberäkning id plus the income warnings to review.")
public class NormberakningResponse {

	@Schema(description = "The id of the normberäkning created in Lifecare FC", examples = "4711")
	private Integer calculationId;

	@Schema(description = "SSBTEK incomes that could not be auto-transferred and must be reviewed", examples = "[\"Bostadstillägg (NOT_ON_WHITELIST)\"]")
	private List<String> unhandledIncomes = new ArrayList<>();

	@Schema(description = "Förmåner whose net income changed beyond the threshold between the periods", examples = "[\"Bostadsbidrag: -23% (jämförelse 2400 → kontroll 1850)\"]")
	private List<String> changeWarnings = new ArrayList<>();

	public static NormberakningResponse create() {
		return new NormberakningResponse();
	}

	public Integer getCalculationId() {
		return calculationId;
	}

	public void setCalculationId(final Integer calculationId) {
		this.calculationId = calculationId;
	}

	public NormberakningResponse withCalculationId(final Integer calculationId) {
		this.calculationId = calculationId;
		return this;
	}

	public List<String> getUnhandledIncomes() {
		return unhandledIncomes;
	}

	public void setUnhandledIncomes(final List<String> unhandledIncomes) {
		this.unhandledIncomes = unhandledIncomes;
	}

	public NormberakningResponse withUnhandledIncomes(final List<String> unhandledIncomes) {
		this.unhandledIncomes = unhandledIncomes;
		return this;
	}

	public List<String> getChangeWarnings() {
		return changeWarnings;
	}

	public void setChangeWarnings(final List<String> changeWarnings) {
		this.changeWarnings = changeWarnings;
	}

	public NormberakningResponse withChangeWarnings(final List<String> changeWarnings) {
		this.changeWarnings = changeWarnings;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final NormberakningResponse that = (NormberakningResponse) o;
		return Objects.equals(calculationId, that.calculationId) && Objects.equals(unhandledIncomes, that.unhandledIncomes) && Objects.equals(changeWarnings, that.changeWarnings);
	}

	@Override
	public int hashCode() {
		return Objects.hash(calculationId, unhandledIncomes, changeWarnings);
	}

	@Override
	public String toString() {
		return "NormberakningResponse{calculationId=" + calculationId + ", unhandledIncomes=" + unhandledIncomes + ", changeWarnings=" + changeWarnings + "}";
	}
}
