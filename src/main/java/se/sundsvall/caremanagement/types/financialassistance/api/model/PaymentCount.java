package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The number of payments on the errand")
public record PaymentCount(
	@Schema(description = "Number of payments on the errand", examples = "2") long count) {
}
