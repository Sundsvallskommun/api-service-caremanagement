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
 * Rejects any municipality-scoped business request whose {@code X-Sent-By} header is missing or not a valid
 * {@link Identifier} ({@code <value>; type=<type>}), so every logged action is attributable — no anonymous or
 * unparseable actors.
 *
 * <p>
 * Only the {@code /{municipalityId}/**} business surface is gated, so infra paths (actuator, api-docs, swagger) are
 * unaffected and the healthcheck keeps passing. CORS preflight ({@code OPTIONS}) is exempt, since the browser cannot
 * attach custom headers. Set {@code caremanagement.identifier.required=false} to disable enforcement without a code
 * change.
 *
 * <p>
 * Validation parses the raw header via {@link Identifier#parse(String)} (which is {@code null} for a missing, blank or
 * incomplete value) rather than reading {@link Identifier#get()}, which the dept44 filter populates leniently and can
 * be non-null for a malformed header.
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
		if (Identifier.parse(request.getHeader(Identifier.HEADER_NAME)) == null) {
			throw Problem.valueOf(BAD_REQUEST, "Missing or malformed required header '" + Identifier.HEADER_NAME
				+ "' — expected e.g. 'joe001doe; type=adAccount' or '<uuid>; type=partyId'");
		}
		return true;
	}
}
