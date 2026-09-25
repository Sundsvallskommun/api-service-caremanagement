package se.sundsvall.caremanagement.eventlog.service;

import java.util.List;
import org.springframework.stereotype.Service;
import se.sundsvall.caremanagement.eventlog.api.model.LifecareAccess;
import se.sundsvall.caremanagement.eventlog.spi.LifecareAccessEntry;
import se.sundsvall.caremanagement.eventlog.spi.LifecareAccessLog;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.support.Identifier;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * Records careM's own Lifecare accesses through the same path as the ones a caller reports over HTTP.
 */
@Service
class LifecareAccessLogService implements LifecareAccessLog {

	private final ErrandEventService errandEventService;

	LifecareAccessLogService(final ErrandEventService errandEventService) {
		this.errandEventService = errandEventService;
	}

	@Override
	public void record(final String municipalityId, final String namespace, final String errandId, final List<LifecareAccessEntry> entries) {
		if (entries.isEmpty()) {
			return;
		}
		errandEventService.recordLifecareAccesses(municipalityId, namespace, errandId, caller(), entries.stream()
			.map(entry -> LifecareAccess.create()
				.withAction(entry.action())
				.withTarget(entry.target())
				.withDescription(entry.description())
				.withLifecareId(entry.lifecareId()))
			.toList());
	}

	private static Identifier caller() {
		final var identifier = Identifier.get();
		if (identifier == null) {
			throw Problem.valueOf(BAD_REQUEST, "Missing or malformed required header '" + Identifier.HEADER_NAME + "'");
		}
		return identifier;
	}
}
