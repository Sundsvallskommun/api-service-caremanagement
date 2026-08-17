package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

/**
 * Result of creating the Lifecare FamilyCare actualisation (case intake): the id of the actualisation created in
 * Lifecare.
 */
@Schema(description = "The created Lifecare actualisation id.")
public class ActualisationResponse {

	@Schema(description = "The id of the actualisation created in Lifecare FamilyCare", examples = "5012")
	private Integer actualisationId;

	public static ActualisationResponse create() {
		return new ActualisationResponse();
	}

	public Integer getActualisationId() {
		return actualisationId;
	}

	public void setActualisationId(final Integer actualisationId) {
		this.actualisationId = actualisationId;
	}

	public ActualisationResponse withActualisationId(final Integer actualisationId) {
		this.actualisationId = actualisationId;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final ActualisationResponse that = (ActualisationResponse) o;
		return Objects.equals(actualisationId, that.actualisationId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(actualisationId);
	}

	@Override
	public String toString() {
		return "ActualisationResponse{actualisationId=" + actualisationId + "}";
	}
}
