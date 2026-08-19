package se.sundsvall.caremanagement.templating.integration;

import generated.se.sundsvall.templating.RenderRequest;
import generated.se.sundsvall.templating.RenderResponse;
import java.util.Base64;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import se.sundsvall.dept44.problem.Problem;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;

/**
 * Thin wrapper around the templating service: sends a {@link RenderRequest} and returns the rendered PDF as decoded
 * bytes. The templating service answers with the PDF BASE64-encoded in {@link RenderResponse#getOutput()}.
 */
@Component
public class TemplatingIntegration {

	private final TemplatingClient templatingClient;

	public TemplatingIntegration(final TemplatingClient templatingClient) {
		this.templatingClient = templatingClient;
	}

	public byte[] renderPdf(final String municipalityId, final RenderRequest request) {
		return Optional.ofNullable(templatingClient.render(municipalityId, request))
			.map(RenderResponse::getOutput)
			.filter(StringUtils::hasText)
			.map(output -> Base64.getDecoder().decode(output))
			.orElseThrow(() -> Problem.valueOf(BAD_GATEWAY, "Templating service returned no output when rendering PDF for municipality '%s'".formatted(municipalityId)));
	}
}
