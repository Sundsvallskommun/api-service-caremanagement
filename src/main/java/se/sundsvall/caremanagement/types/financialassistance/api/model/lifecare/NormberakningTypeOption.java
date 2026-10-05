package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

/**
 * A selectable norm, income or cost type: the code a row stores and the label shown.
 */
@Schema(description = "A selectable norm, income or cost type.")
public class NormberakningTypeOption {

	@Schema(description = "The code: a Lifecare id once the beräkning is in Lifecare, careM's code before", examples = "3")
	private String code;

	@Schema(description = "The label", examples = "Boendekostnad")
	private String displayName;

	public static NormberakningTypeOption create() {
		return new NormberakningTypeOption();
	}

	public String getCode() {
		return code;
	}

	public void setCode(final String code) {
		this.code = code;
	}

	public NormberakningTypeOption withCode(final String code) {
		this.code = code;
		return this;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(final String displayName) {
		this.displayName = displayName;
	}

	public NormberakningTypeOption withDisplayName(final String displayName) {
		this.displayName = displayName;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final NormberakningTypeOption that = (NormberakningTypeOption) o;
		return Objects.equals(code, that.code) && Objects.equals(displayName, that.displayName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(code, displayName);
	}

	@Override
	public String toString() {
		return "NormberakningTypeOption{" +
			"code='" + code + '\'' +
			", displayName='" + displayName + '\'' +
			'}';
	}
}
