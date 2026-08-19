package se.sundsvall.caremanagement.errandtypes.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

/**
 * One allowed decision outcome (decision alternatives) for an errand type — the options a caseworker may pick from when
 * recording a {@code Decision} on the case. {@code carriesAmount} tells the frontend whether the option expects an
 * amount
 * (e.g. a granted amount) or implies a zero amount (e.g. a rejection), so the Decision form can react.
 */
@Schema(description = "An allowed decision outcome (decision alternatives) for an errand type.")
public class DecisionOption {

	@Schema(description = "The decision outcome code, stored on the Decision row's value", examples = "BIFALL")
	private String code;

	@Schema(description = "Human-readable label for the outcome", examples = "Bifall")
	private String displayName;

	@Schema(description = "Whether the outcome carries an amount — true for outcomes that grant an amount, false for ones that imply 0 (e.g. a rejection)", examples = "true")
	private boolean carriesAmount;

	public static DecisionOption create() {
		return new DecisionOption();
	}

	public String getCode() {
		return code;
	}

	public void setCode(final String code) {
		this.code = code;
	}

	public DecisionOption withCode(final String code) {
		this.code = code;
		return this;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(final String displayName) {
		this.displayName = displayName;
	}

	public DecisionOption withDisplayName(final String displayName) {
		this.displayName = displayName;
		return this;
	}

	public boolean isCarriesAmount() {
		return carriesAmount;
	}

	public void setCarriesAmount(final boolean carriesAmount) {
		this.carriesAmount = carriesAmount;
	}

	public DecisionOption withCarriesAmount(final boolean carriesAmount) {
		this.carriesAmount = carriesAmount;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final DecisionOption that = (DecisionOption) o;
		return carriesAmount == that.carriesAmount && Objects.equals(code, that.code) && Objects.equals(displayName, that.displayName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(code, displayName, carriesAmount);
	}

	@Override
	public String toString() {
		return "DecisionOption{code='" + code + "', displayName='" + displayName + "', carriesAmount=" + carriesAmount + '}';
	}
}
