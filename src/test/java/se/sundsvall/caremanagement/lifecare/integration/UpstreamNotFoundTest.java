package se.sundsvall.caremanagement.lifecare.integration;

import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.lifecare.integration.integrator.BufferingProblemErrorDecoder;
import se.sundsvall.dept44.configuration.feign.decoder.ProblemErrorDecoder;
import se.sundsvall.dept44.problem.Problem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.caremanagement.lifecare.integration.DecodedProblems.decode;

class UpstreamNotFoundTest {

	private static final String INTEGRATOR_NOT_FOUND = """
		{"title":"Not Found","status":404,"detail":"No person found for partyId '6a5c3d18-1f3b-4c2a-9d9e-2b7f4a1c8e55'"}""";
	private static final String INTEGRATOR_PARTY_NOT_FOUND = """
		{"title":"Not Found","status":404,"detail":"No person number found for partyId '6a5c3d18-1f3b-4c2a-9d9e-2b7f4a1c8e55'"}""";
	private static final String FAMILYCARE_NOT_FOUND = """
		{"Code":"NotFound","Message":"Not found: Person"}""";

	private final ProblemErrorDecoder familyCareDecoder = new ProblemErrorDecoder("lifecare-familycare");
	private final BufferingProblemErrorDecoder integratorDecoder = new BufferingProblemErrorDecoder("lifecare-integrator");

	@Test
	void theDecoderDoesNotSurfaceA404AsNotFound() {
		// The premise of the class: the status of the thrown problem is BAD_GATEWAY, so it cannot be read from getStatus().
		final var problem = decode(integratorDecoder, 404, INTEGRATOR_NOT_FOUND);

		assertThat(problem.getStatus()).isEqualTo(BAD_GATEWAY).isNotEqualTo(NOT_FOUND);
	}

	@Test
	void recognisesAnIntegratorNotFound() {
		assertThat(UpstreamNotFound.matches(decode(integratorDecoder, 404, INTEGRATOR_NOT_FOUND))).isTrue();
	}

	@Test
	void recognisesAFamilyCareNotFoundWhoseBodyIsNotAProblem() {
		assertThat(UpstreamNotFound.matches(decode(familyCareDecoder, 404, FAMILYCARE_NOT_FOUND))).isTrue();
	}

	@Test
	void recognisesANotFoundWithoutABody() {
		assertThat(UpstreamNotFound.matches(decode(familyCareDecoder, 404, null))).isTrue();
		assertThat(UpstreamNotFound.matches(decode(integratorDecoder, 404, ""))).isTrue();
	}

	@Test
	void doesNotRecogniseOtherClientErrors() {
		assertThat(UpstreamNotFound.matches(decode(integratorDecoder, 400, INTEGRATOR_NOT_FOUND))).isFalse();
		assertThat(UpstreamNotFound.matches(decode(integratorDecoder, 403, null))).isFalse();
		assertThat(UpstreamNotFound.matches(decode(familyCareDecoder, 409, FAMILYCARE_NOT_FOUND))).isFalse();
	}

	@Test
	void doesNotRecogniseServerErrors() {
		assertThat(UpstreamNotFound.matches(decode(integratorDecoder, 500, INTEGRATOR_NOT_FOUND))).isFalse();
		assertThat(UpstreamNotFound.matches(decode(familyCareDecoder, 502, null))).isFalse();
		assertThat(UpstreamNotFound.matches(decode(familyCareDecoder, 503, FAMILYCARE_NOT_FOUND))).isFalse();
	}

	@Test
	void doesNotRecogniseAProblemThatMerelyMentionsA404() {
		assertThat(UpstreamNotFound.matches(Problem.valueOf(BAD_GATEWAY, "the upstream said 404"))).isFalse();
		assertThat(UpstreamNotFound.matches(Problem.valueOf(NOT_FOUND, "person not found"))).isFalse();
	}

	@Test
	void doesNotRecogniseAProblemWithoutADetail() {
		assertThat(UpstreamNotFound.matches(Problem.valueOf(BAD_GATEWAY))).isFalse();
	}

	@Test
	void recognisesANotFoundWithTheExpectedExplanation() {
		assertThat(UpstreamNotFound.matches(decode(integratorDecoder, 404, INTEGRATOR_NOT_FOUND), "No person found for partyId")).isTrue();
	}

	@Test
	void doesNotRecogniseANotFoundWithAnotherExplanation() {
		// Party cannot resolve the partyId: also a 404, but not the answer that Lifecare holds no such person.
		assertThat(UpstreamNotFound.matches(decode(integratorDecoder, 404, INTEGRATOR_PARTY_NOT_FOUND), "No person found for partyId")).isFalse();
		assertThat(UpstreamNotFound.matches(decode(integratorDecoder, 404, null), "No person found for partyId")).isFalse();
	}

	@Test
	void doesNotRecogniseTheExpectedExplanationOnAnotherStatus() {
		assertThat(UpstreamNotFound.matches(decode(integratorDecoder, 500, INTEGRATOR_NOT_FOUND), "No person found for partyId")).isFalse();
	}
}
