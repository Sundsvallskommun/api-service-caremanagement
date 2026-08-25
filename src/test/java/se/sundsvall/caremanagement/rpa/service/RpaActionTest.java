package se.sundsvall.caremanagement.rpa.service;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.rpa.api.model.RpaTaskRequest;
import se.sundsvall.dept44.common.validators.annotation.MemberOf;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards that {@link RpaTaskRequest#getAction()} is validated against the {@link RpaAction} enum, and that its
 * documented {@code @Schema} allow-list stays in sync with the enum — so the API validation, the OpenAPI doc and the
 * action catalogue can't drift apart.
 */
class RpaActionTest {

	@Test
	void actionIsValidatedAgainstTheEnum() throws Exception {
		final var memberOf = RpaTaskRequest.class.getDeclaredField("action").getAnnotation(MemberOf.class);

		assertThat(memberOf).isNotNull();
		assertThat(memberOf.value()).isEqualTo(RpaAction.class);
	}

	@Test
	void schemaAllowableValuesMatchTheEnum() throws Exception {
		final var schema = RpaTaskRequest.class.getDeclaredField("action").getAnnotation(Schema.class);

		assertThat(schema).isNotNull();
		assertThat(schema.allowableValues()).containsExactlyInAnyOrder(
			Arrays.stream(RpaAction.values()).map(Enum::name).toArray(String[]::new));
	}
}
