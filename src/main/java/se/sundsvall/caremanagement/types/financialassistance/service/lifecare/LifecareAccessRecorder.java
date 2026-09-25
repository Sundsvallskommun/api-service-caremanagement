package se.sundsvall.caremanagement.types.financialassistance.service.lifecare;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.sundsvall.caremanagement.eventlog.spi.LifecareAccessEntry;
import se.sundsvall.caremanagement.eventlog.spi.LifecareAccessLog;

/**
 * Writes an errand's Lifecare accesses to its access log.
 *
 * <p>
 * The two directions fail differently, as verksamheten's logging rule requires. A read that cannot be logged is not
 * served: the data has not left careM yet, so refusing costs nothing but a retry. A write that cannot be logged is
 * already done in Lifecare; failing the request then would invite the caseworker to write it a second time, so the
 * failure is logged and the write stands.
 * </p>
 */
@Component
public class LifecareAccessRecorder {

	private static final Logger LOG = LoggerFactory.getLogger(LifecareAccessRecorder.class);

	private final LifecareAccessLog accessLog;

	LifecareAccessRecorder(final LifecareAccessLog accessLog) {
		this.accessLog = accessLog;
	}

	/**
	 * Logs a read. Throws when it cannot be logged, and the read must then not be served.
	 *
	 * @param errand      the errand
	 * @param target      what was read
	 * @param description a Swedish summary for the log
	 */
	public void read(final LifecareErrand errand, final String target, final String description) {
		read(errand, target, description, null);
	}

	public void read(final LifecareErrand errand, final String target, final String description, final String lifecareId) {
		accessLog.record(errand.municipalityId(), errand.namespace(), errand.errandId(),
			List.of(new LifecareAccessEntry(LifecareAccessEntry.READ, target, description, lifecareId)));
	}

	/**
	 * Logs a write already made. Never throws.
	 *
	 * @param errand      the errand
	 * @param action      CREATE, UPDATE or DELETE
	 * @param target      what was written
	 * @param description a Swedish summary for the log
	 * @param lifecareId  the Lifecare record written, when known
	 */
	public void written(final LifecareErrand errand, final String action, final String target, final String description, final String lifecareId) {
		try {
			accessLog.record(errand.municipalityId(), errand.namespace(), errand.errandId(),
				List.of(new LifecareAccessEntry(action, target, description, lifecareId)));
		} catch (final RuntimeException e) {
			LOG.error("Could not log a Lifecare {} of {} on errand {} ({})", action, target, errand.errandId(), e.getClass().getSimpleName());
		}
	}
}
