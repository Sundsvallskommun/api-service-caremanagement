package se.sundsvall.caremanagement.eventlog.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.support.Identifier;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * Rejects any municipality-scoped business request that arrives without a parsable {@code X-Sent-By} identity, so every
 * action on the API is attributable in the event log — no more anonymous (null-actor) rows.
 *
 * <p>
 * Guardrails: only the {@code /{municipalityId}/**} business surface is gated, so infra paths (actuator/health,
 * api-docs, swagger) are never affected and the platform healthcheck keeps passing. CORS preflight ({@code OPTIONS})
 * requests are exempt, since the browser cannot attach custom headers to them. Disabled in test profiles via
 * {@code caremanagement.identifier.required=false}; flip that property to turn enforcement off in any environment
 * without a code change.
 */
@Component
class RequireIdentifierInterceptor implements HandlerInterceptor {

	private final boolean required;

	RequireIdentifierInterceptor(@Value("${caremanagement.identifier.required:true}") final boolean required) {
		this.required = required;
	}

	@Override
	public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler) {
		if (!required || "OPTIONS".equals(request.getMethod())) {
			return true;
		}
		if (Identifier.get() == null) {
			throw Problem.valueOf(BAD_REQUEST, "Missing or malformed required header '" + Identifier.HEADER_NAME
				+ "' — expected e.g. 'joe001doe; type=adAccount' or '<uuid>; type=partyId'");
		}
		return true;
	}
}
