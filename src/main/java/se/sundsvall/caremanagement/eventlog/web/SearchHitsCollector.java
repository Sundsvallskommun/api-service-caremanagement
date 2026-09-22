package se.sundsvall.caremanagement.eventlog.web;

import java.util.List;
import java.util.Objects;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import se.sundsvall.caremanagement.core.api.model.Errand;
import se.sundsvall.caremanagement.core.api.model.FindErrandsResponse;

import static java.util.Optional.ofNullable;

/**
 * Captures the errand ids a search actually returned, so {@link ErrandEventInterceptor} can log the hits the
 * caseworker saw.
 *
 * <p>
 * Verksamhetens regelverk (revision 2026-09-22): <em>”Det måste loggas när man gör ett stort 'sök' på alla träffar
 * man har sett, alltså första sidan om man inte har bläddrat, loggas på ärendet men även på användare.”</em> The
 * “träffar man har sett” part is why this class exists: the hits are in the response body, and by the time
 * {@code afterCompletion} runs the body has been serialised and is gone. Nothing else in the request carries them —
 * the query is a filter, not a list of results.
 * </p>
 *
 * <p>
 * A {@code ResponseBodyAdvice} sees the body just before it is written, while the request thread still holds the
 * dept44 {@code Identifier} and {@code RequestId}. It stashes the ids on the request and does nothing else; the
 * interceptor stays the single place that writes rows, so there is one audit-write path rather than two.
 * </p>
 *
 * <p>
 * Deliberately no-ops on anything but a search response body, and never touches the body itself.
 * </p>
 */
@ControllerAdvice
class SearchHitsCollector implements ResponseBodyAdvice<Object> {

	/** Request attribute carrying the ids of the errands a search returned. */
	static final String SEARCH_HITS_ATTRIBUTE = SearchHitsCollector.class.getName() + ".searchHits";

	@Override
	public boolean supports(final MethodParameter returnType, final Class<? extends org.springframework.http.converter.HttpMessageConverter<?>> converterType) {
		return true;
	}

	@Override
	public Object beforeBodyWrite(@Nullable final Object body, final MethodParameter returnType, final MediaType selectedContentType,
		final Class<? extends org.springframework.http.converter.HttpMessageConverter<?>> selectedConverterType,
		final ServerHttpRequest request, final ServerHttpResponse response) {

		if ((body instanceof final FindErrandsResponse hits) && (request instanceof final ServletServerHttpRequest servletRequest)) {
			servletRequest.getServletRequest().setAttribute(SEARCH_HITS_ATTRIBUTE, errandIds(hits));
		}
		return body;
	}

	/**
	 * The ids of the errands on the returned page — “de träffar man har sett”. A page the caseworker never scrolled
	 * past is exactly one page of ids, which is the regelverk's own definition.
	 */
	private static List<String> errandIds(final FindErrandsResponse hits) {
		return ofNullable(hits.getErrands()).orElseGet(List::of).stream()
			.filter(Objects::nonNull)
			.map(Errand::getId)
			.filter(Objects::nonNull)
			.distinct()
			.toList();
	}
}
