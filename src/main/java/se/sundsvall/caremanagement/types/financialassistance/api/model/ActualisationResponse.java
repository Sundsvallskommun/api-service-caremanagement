package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

/**
 * Result of creating the Lifecare FC aktualisering (case intake): the id of the aktualisering created in Lifecare.
 */
@Schema(description = "The created Lifecare aktualisering id.")
public class ActualisationResponse {

	@Schema(description = "The id of the aktualisering created in Lifecare FC", examples = "5012")
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
