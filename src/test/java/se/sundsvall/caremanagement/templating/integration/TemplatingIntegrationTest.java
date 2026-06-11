package se.sundsvall.caremanagement.templating.integration;

import generated.se.sundsvall.templating.RenderRequest;
import generated.se.sundsvall.templating.RenderResponse;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;

@ExtendWith(MockitoExtension.class)
class TemplatingIntegrationTest {

	@Mock
	private TemplatingClient templatingClientMock;

	@Mock
	private RenderResponse renderResponseMock;

	@InjectMocks
	private TemplatingIntegration integration;

	@Test
	void renderPdfDecodesBase64Output() {
		final var pdfBytes = "%PDF-1.4 demo".getBytes();
		when(templatingClientMock.render(eq("2281"), any(RenderRequest.class))).thenReturn(renderResponseMock);
		when(renderResponseMock.getOutput()).thenReturn(Base64.getEncoder().encodeToString(pdfBytes));

		final var result = integration.renderPdf("2281", new RenderRequest());

		assertThat(result).isEqualTo(pdfBytes);
	}

	@Test
	void renderPdfNullResponseThrowsBadGateway() {
		when(templatingClientMock.render(eq("2281"), any(RenderRequest.class))).thenReturn(null);

		assertThatThrownBy(() -> integration.renderPdf("2281", new RenderRequest()))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY);
	}

	@Test
	void renderPdfBlankOutputThrowsBadGateway() {
		when(templatingClientMock.render(eq("2281"), any(RenderRequest.class))).thenReturn(renderResponseMock);
		when(renderResponseMock.getOutput()).thenReturn(" ");

		assertThatThrownBy(() -> integration.renderPdf("2281", new RenderRequest()))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY);
	}
}
