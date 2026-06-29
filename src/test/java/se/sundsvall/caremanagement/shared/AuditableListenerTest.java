package se.sundsvall.caremanagement.shared;

import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.metadata.integration.db.model.LookupEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class AuditableListenerTest {

	private final AuditableListener listener = new AuditableListener();

	@Test
	void onCreateSetsCreatedOnAuditableInstance() {
		final var entity = new LookupEntity();

		listener.onCreate(entity);

		assertThat(entity.getCreated()).isNotNull();
		assertThat(entity.getModified()).isNull();
	}

	@Test
	void onUpdateSetsModifiedOnAuditableInstance() {
		final var entity = new LookupEntity();

		listener.onUpdate(entity);

		assertThat(entity.getModified()).isNotNull();
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
