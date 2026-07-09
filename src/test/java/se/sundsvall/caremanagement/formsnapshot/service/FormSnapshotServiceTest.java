package se.sundsvall.caremanagement.formsnapshot.service;

import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.formsnapshot.integration.db.FormSnapshotRepository;
import se.sundsvall.caremanagement.formsnapshot.integration.db.model.FormSnapshotEntity;
import se.sundsvall.dept44.problem.ThrowableProblem;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class FormSnapshotServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "EB";
	private static final String ERRAND_ID = "errand-1";
	private static final String SLUG = "financial-assistance-new";
	private static final String VALID_PAYLOAD = "{\"schemaVersion\":\"form-snapshot/1\",\"title\":\"Ansökan\",\"sections\":[{\"id\":\"household\",\"title\":\"Hushåll\"}]}";

	@Mock
	private FormSnapshotRepository repositoryMock;

	private FormSnapshotService service;

	@BeforeEach
	void setUp() {
		service = new FormSnapshotService(repositoryMock, new ObjectMapper());
	}

	@Test
	void captureStoresVerbatimPayloadAndHash() {
		when(repositoryMock.existsByErrandId(ERRAND_ID)).thenReturn(false);
		when(repositoryMock.save(any(FormSnapshotEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.saveErrandFormSnapshot(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, SLUG, VALID_PAYLOAD);

		final ArgumentCaptor<FormSnapshotEntity> captor = ArgumentCaptor.forClass(FormSnapshotEntity.class);
		verify(repositoryMock).save(captor.capture());
		final var entity = captor.getValue();
		assertThat(entity.getErrandId()).isEqualTo(ERRAND_ID);
		assertThat(entity.getTypeSlug()).isEqualTo(SLUG);
		assertThat(entity.getSchemaVersion()).isEqualTo("form-snapshot/1");
		assertThat(entity.getPayload()).isEqualTo(VALID_PAYLOAD);
		assertThat(entity.getContentHash()).hasSize(64);
		assertThat(entity.getCreated()).isNotNull();
	}

	@ParameterizedTest(name = "rejects {2}")
	@MethodSource
	void captureRejects(final String payload, final boolean alreadyExists, final String reason) {
		if (alreadyExists) {
			when(repositoryMock.existsByErrandId(ERRAND_ID)).thenReturn(true);
		}

		assertThatThrownBy(() -> service.saveErrandFormSnapshot(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, SLUG, payload))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_REQUEST);
		verify(repositoryMock, never()).save(any());
	}

	static Stream<Arguments> captureRejects() {
		return Stream.of(
			arguments("  ", false, "a blank payload"),
			arguments("{not json", false, "malformed JSON"),
			arguments("{\"sections\":[{\"id\":\"x\"}]}", false, "a missing schema version"),
			arguments(VALID_PAYLOAD, true, "a snapshot that already exists"));
	}

	@Test
	void captureRejectsEmptySections() {
		assertThatThrownBy(() -> service.saveErrandFormSnapshot(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, SLUG, "{\"schemaVersion\":\"form-snapshot/1\",\"sections\":[]}"))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_REQUEST);
		verify(repositoryMock, never()).save(any());
	}

	@Test
	void readReturnsParsedSnapshot() {
		when(repositoryMock.findByErrandId(ERRAND_ID))
			.thenReturn(Optional.of(FormSnapshotEntity.create().withErrandId(ERRAND_ID).withPayload(VALID_PAYLOAD)));

		final var snapshot = service.readErrandFormSnapshot(ERRAND_ID);

		assertThat(snapshot.getSchemaVersion()).isEqualTo("form-snapshot/1");
		assertThat(snapshot.getTitle()).isEqualTo("Ansökan");
		assertThat(snapshot.getSections()).hasSize(1);
		assertThat(snapshot.getSections().getFirst().getId()).isEqualTo("household");
	}

	@Test
	void readThrowsNotFoundWhenMissing() {
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.readErrandFormSnapshot(ERRAND_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);
	}
}
