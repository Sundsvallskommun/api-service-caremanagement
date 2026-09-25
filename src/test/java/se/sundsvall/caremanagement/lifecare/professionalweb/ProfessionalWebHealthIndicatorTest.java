package se.sundsvall.caremanagement.lifecare.professionalweb;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.health.contributor.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfessionalWebHealthIndicatorTest {

	@Mock
	private ProfessionalWebSession session;

	@Test
	void unconfigured() {
		final var health = new ProfessionalWebHealthIndicator(properties(null), session).health();

		assertThat(health.getStatus()).isEqualTo(Status.UNKNOWN);
		assertThat(health.getDetails()).containsEntry("configured", false);
	}

	@Test
	void established() {
		when(session.isEstablished()).thenReturn(true);

		assertThat(new ProfessionalWebHealthIndicator(properties("https://lifecare"), session).health().getStatus()).isEqualTo(Status.UP);
	}

	@Test
	void noSessionYet() {
		when(session.isEstablished()).thenReturn(false);

		assertThat(new ProfessionalWebHealthIndicator(properties("https://lifecare"), session).health().getStatus()).isEqualTo(Status.UNKNOWN);
	}

	private static ProfessionalWebProperties properties(final String url) {
		return new ProfessionalWebProperties(url, "d", "a", "saml", null, "u", "p", Duration.ofMinutes(20), 1, 1, "x", "y", null);
	}
}
