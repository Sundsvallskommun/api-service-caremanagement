package se.sundsvall.caremanagement.eneo.integration;

import generated.se.sundsvall.eneo.AskAssistant;
import generated.se.sundsvall.eneo.AskResponse;
import generated.se.sundsvall.eneo.FilePublic;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;

@ExtendWith(MockitoExtension.class)
class EneoIntegrationTest {

	@Mock
	private EneoClient eneoClientMock;

	@InjectMocks
	private EneoIntegration integration;

	@Test
	void askAssistantReturnsResponse() {
		final var assistantId = UUID.randomUUID();
		final var request = new AskAssistant();
		final var response = new AskResponse();
		when(eneoClientMock.askAssistant(assistantId, request)).thenReturn(response);

		assertThat(integration.askAssistant(assistantId, request)).isSameAs(response);
	}

	@Test
	void askAssistantTranslatesFailureToBadGateway() {
		final var assistantId = UUID.randomUUID();
		final var request = new AskAssistant();
		when(eneoClientMock.askAssistant(assistantId, request)).thenThrow(new RuntimeException("boom"));

		assertThatThrownBy(() -> integration.askAssistant(assistantId, request))
			.isInstanceOf(se.sundsvall.dept44.problem.ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY);
	}

	@Test
	void uploadFileReturnsBody() {
		final var file = new ByteArrayMultipartFile("upload_file", "doc.pdf", "application/pdf", "x".getBytes());
		final var filePublic = new FilePublic();
		when(eneoClientMock.uploadFile(file)).thenReturn(ResponseEntity.ok(filePublic));

		assertThat(integration.uploadFile(file)).isSameAs(filePublic);
	}

	@Test
	void uploadFileTranslatesFailureToBadGateway() {
		final var file = new ByteArrayMultipartFile("upload_file", "doc.pdf", "application/pdf", "x".getBytes());
		when(eneoClientMock.uploadFile(file)).thenThrow(new RuntimeException("boom"));

		assertThatThrownBy(() -> integration.uploadFile(file))
			.isInstanceOf(se.sundsvall.dept44.problem.ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY);
	}

	@Test
	void deleteFileCallsClient() {
		final var fileId = UUID.randomUUID();
		when(eneoClientMock.deleteFile(fileId)).thenReturn(ResponseEntity.noContent().build());

		integration.deleteFile(fileId);

		verify(eneoClientMock).deleteFile(fileId);
	}

	@Test
	void deleteFileSwallowsFailure() {
		final var fileId = UUID.randomUUID();
		when(eneoClientMock.deleteFile(fileId)).thenThrow(new RuntimeException("boom"));

		assertThatNoException().isThrownBy(() -> integration.deleteFile(fileId));
		verify(eneoClientMock).deleteFile(fileId);
	}
}
