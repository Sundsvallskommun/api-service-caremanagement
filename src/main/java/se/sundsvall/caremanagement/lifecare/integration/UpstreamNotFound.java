package se.sundsvall.caremanagement.lifecare.integration;

import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.springframework.util.StringUtils.hasText;

/**
 * Recognises an upstream <em>404 Not Found</em> in the problem the Feign error decoders of the Lifecare clients throw.
 *
 * <p>
 * Neither client bypasses 404 in its error decoder, so a 404 does <b>not</b> arrive as a problem with status
 * {@code NOT_FOUND}: dept44's decoder turns every 4xx into a {@code BAD_GATEWAY} problem whose detail is the
 * upstream's own explanation, rendered as {@code <client> error: {detail=..., status=404 Not Found, title=Not Found}}.
 * The status is therefore read from that rendered detail. Should dept44 ever change the format, nothing matches and the
 * 404 goes back to being a {@code BAD_GATEWAY} failure — which for every caller here is the safe direction (the
 * protected-identity gate then fails closed), and the tests that build the problem with the real decoders fail first.
 *
 * <p>
 * A 404 is only ever a missing person for the call that asks for one person by identity; the caller decides that, this
 * class only reads the status.
 */
public final class UpstreamNotFound {

	/** How dept44's {@code AbstractErrorDecoder} renders the response status inside the problem detail. */
	static final String STATUS_404 = "status=404 Not Found";

	private UpstreamNotFound() {
		// utility class
	}

	/**
	 * Whether the upstream answered 404.
	 *
	 * @param  problem the problem thrown by the Feign client
	 * @return         {@code true} when the detail says the response status was 404 Not Found
	 */
	public static boolean matches(final ThrowableProblem problem) {
		return hasText(problem.getDetail()) && problem.getDetail().contains(STATUS_404);
	}

	/**
	 * Whether the upstream answered 404 <em>and</em> explained itself with the given text. For an upstream that has more
	 * than one reason to answer 404, so that only the one meant is taken as an answer.
	 *
	 * @param  problem        the problem thrown by the Feign client
	 * @param  detailFragment the text the upstream's own explanation must contain
	 * @return                {@code true} when the response status was 404 Not Found and the explanation contains the text
	 */
	public static boolean matches(final ThrowableProblem problem, final String detailFragment) {
		return matches(problem) && problem.getDetail().contains(detailFragment);
	}
}
