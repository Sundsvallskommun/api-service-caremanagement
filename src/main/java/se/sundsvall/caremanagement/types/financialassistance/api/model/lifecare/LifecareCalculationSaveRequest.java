package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

/**
 * How the errand's normberäkning is saved in Lifecare.
 */
@Schema(description = "How the errand's normberäkning is saved in Lifecare.")
public class LifecareCalculationSaveRequest {

	@Schema(description = "Save it as slutlig: Lifecare then allows no further change", examples = "false")
	private Boolean finalize;

	public static LifecareCalculationSaveRequest create() {
		return new LifecareCalculationSaveRequest();
	}

	public Boolean getFinalize() {
		return finalize;
	}

	public void setFinalize(final Boolean finalize) {
		this.finalize = finalize;
	}

	public LifecareCalculationSaveRequest withFinalize(final Boolean finalize) {
		this.finalize = finalize;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final LifecareCalculationSaveRequest that = (LifecareCalculationSaveRequest) o;
		return Objects.equals(finalize, that.finalize);
	}

	@Override
	public int hashCode() {
		return Objects.hash(finalize);
	}

	@Override
	public String toString() {
		return "LifecareCalculationSaveRequest{" +
			"finalize=" + finalize +
			'}';
	}
}
