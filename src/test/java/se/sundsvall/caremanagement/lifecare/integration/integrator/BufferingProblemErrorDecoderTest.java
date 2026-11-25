package se.sundsvall.caremanagement.lifecare.integration.integrator;

import feign.Request;
import feign.Response;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The integrator answers in RFC 9457 and names Lifecare's own complaint in {@code detail}. dept44 reads the body
 * twice, so without buffering the second read is empty and the caller is told only "Unknown error" — which is what
 * every failed Lifecare write reported until this decoder existed.
 */
class BufferingProblemErrorDecoderTest {

	private static final String BODY = """
		{"detail":"lifecare-fc error: {detail=Saknar norm för angiven hushållsstorlek, status=400 Bad Request}",\
		"status":502,"title":"Bad Gateway"}""";

	private static Response response(final String body) {
		return Response.builder()
			.status(502)
			.reason("Bad Gateway")
			.request(Request.create(Request.HttpMethod.POST, "/2281/calculations", Map.of(), new byte[0], StandardCharsets.UTF_8, null))
			.headers(Map.of("content-type", java.util.List.of("application/problem+json")))
			.body(body, StandardCharsets.UTF_8)
			.build();
	}

	/** A streamed body is what OkHttp hands Feign in production, and the only shape that can reproduce the bug. */
	private static Response streamedResponse(final String body) {
		final var bytes = body.getBytes(StandardCharsets.UTF_8);
		return response(BODY).toBuilder()
			.body(new java.io.ByteArrayInputStream(bytes), bytes.length)
			.build();
	}

	@Test
	void carriesTheIntegratorsExplanationInsteadOfUnknownError() {
		final var streamed = streamedResponse(BODY);
		// Guards the test itself: a repeatable body would let this pass with or without the fix.
		assertThat(streamed.body().isRepeatable()).isFalse();

		final var decoded = new BufferingProblemErrorDecoder("lifecare-integrator").decode("post", streamed);

		assertThat(decoded).isInstanceOf(ThrowableProblem.class);
		assertThat(decoded.getMessage())
			.contains("Saknar norm för angiven hushållsstorlek")
			.doesNotContain("Unknown error");
	}

	/** A response without a body has nothing to buffer, and must survive decoding rather than throw. */
	@Test
	void leavesABodilessResponseAlone() {
		final var bodiless = response(BODY).toBuilder().body((Response.Body) null).build();

		assertThat(BufferingProblemErrorDecoder.withRepeatableBody(bodiless)).isSameAs(bodiless);
	}

	/**
	 * A body that cannot be read leaves the response untouched: the point is to enrich the error being decoded, never
	 * to replace it with a failure to read it.
	 */
	@Test
	void keepsTheOriginalResponseWhenTheBodyCannotBeRead() throws IOException {
		final var unreadable = mock(Response.Body.class);
		when(unreadable.isRepeatable()).thenReturn(false);
		when(unreadable.asInputStream()).thenThrow(new IOException("socket gone"));
		final var broken = response(BODY).toBuilder().body(unreadable).build();

		assertThat(BufferingProblemErrorDecoder.withRepeatableBody(broken)).isSameAs(broken);
	}

	@Test
	void leavesAnAlreadyRepeatableBodyAlone() {
		final var original = response(BODY);

		assertThat(BufferingProblemErrorDecoder.withRepeatableBody(original)).isSameAs(original);
	}
}
