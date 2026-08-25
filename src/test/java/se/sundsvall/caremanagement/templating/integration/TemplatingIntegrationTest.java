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

	private static final String MUNICIPALITY_ID = "2281";

	@Mock
	private TemplatingClient templatingClientMock;

	@Mock
	private RenderResponse renderResponseMock;

	@InjectMocks
	private TemplatingIntegration integration;

	@Test
	void renderPdfDecodesBase64Output() {
		final var pdfBytes = "%PDF-1.4 demo".getBytes();
		when(templatingClientMock.render(eq(MUNICIPALITY_ID), any(RenderRequest.class))).thenReturn(renderResponseMock);
		when(renderResponseMock.getOutput()).thenReturn(Base64.getEncoder().encodeToString(pdfBytes));

		final var result = integration.renderPdf(MUNICIPALITY_ID, new RenderRequest());

		assertThat(result).isEqualTo(pdfBytes);
	}

	@Test
	void renderPdfNullResponseThrowsBadGateway() {
		when(templatingClientMock.render(eq(MUNICIPALITY_ID), any(RenderRequest.class))).thenReturn(null);

		assertThatThrownBy(() -> integration.renderPdf(MUNICIPALITY_ID, new RenderRequest()))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY)
			.extracting(throwable -> ((ThrowableProblem) throwable).getDetail())
			.isEqualTo("Templating service returned no output when rendering PDF for municipality '2281'");
	}

	@Test
	void renderPdfBlankOutputThrowsBadGateway() {
		when(templatingClientMock.render(eq(MUNICIPALITY_ID), any(RenderRequest.class))).thenReturn(renderResponseMock);
		when(renderResponseMock.getOutput()).thenReturn(" ");

		assertThatThrownBy(() -> integration.renderPdf(MUNICIPALITY_ID, new RenderRequest()))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY)
			.extracting(throwable -> ((ThrowableProblem) throwable).getDetail())
			.isEqualTo("Templating service returned no output when rendering PDF for municipality '2281'");
	}
}
