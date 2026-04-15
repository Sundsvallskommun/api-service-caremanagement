package se.sundsvall.caremanagement.integration.db.model;

import org.junit.jupiter.api.Test;

import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.within;

class AuditableListenerTest {

	private final AuditableListener listener = new AuditableListener();

	@Test
	void onCreateSetsCreatedOnAuditableInstance() {
		final var entity = new LookupEntity();

		listener.onCreate(entity);

		assertThat(entity.getCreated()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity.getModified()).isNull();
	}

	@Test
	void onUpdateSetsModifiedOnAuditableInstance() {
		final var entity = new LookupEntity();

		listener.onUpdate(entity);

		assertThat(entity.getModified()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity.getCreated()).isNull();
	}

	@Test
	void onCreateIsNoOpForNonAuditableInstance() {
		assertThatNoException().isThrownBy(() -> {
			listener.onCreate(new Object());
			listener.onUpdate(new Object());
		});
	}
}
