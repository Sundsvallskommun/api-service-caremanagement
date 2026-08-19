package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The number of monitorings on the errand")
public record MonitoringCount(
	@Schema(description = "Number of monitorings on the errand", examples = "2") long count) {
}
