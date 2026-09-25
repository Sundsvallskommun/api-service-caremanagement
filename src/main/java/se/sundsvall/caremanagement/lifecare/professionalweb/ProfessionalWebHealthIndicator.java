package se.sundsvall.caremanagement.lifecare.professionalweb;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Reports whether careM holds a Lifecare ProfessionalWeb session, without calling Lifecare.
 *
 * <p>
 * Deliberately passive: a health probe that signed in would put the integration account's password on the wire every
 * few seconds. No session is reported as UNKNOWN rather than DOWN, because the session is established lazily on the
 * first call and a quiet instance simply has not needed one yet. No cookie value ever appears here.
 * </p>
 */
@Component("lifecareProfessionalWeb")
@ConditionalOnProperty(name = ProfessionalWebTransport.PROVIDER_PROPERTY, havingValue = "direct", matchIfMissing = true)
class ProfessionalWebHealthIndicator implements HealthIndicator {

	private final ProfessionalWebProperties properties;
	private final ProfessionalWebSession session;

	ProfessionalWebHealthIndicator(final ProfessionalWebProperties properties, final ProfessionalWebSession session) {
		this.properties = properties;
		this.session = session;
	}

	@Override
	public Health health() {
		if (!properties.isConfigured()) {
			return Health.unknown().withDetail("configured", false).build();
		}
		if (session.isEstablished()) {
			return Health.up().withDetail("session", "established").build();
		}
		return Health.unknown().withDetail("session", "none yet").build();
	}
}
