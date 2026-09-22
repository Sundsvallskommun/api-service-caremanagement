package se.sundsvall.caremanagement.eventlog.web;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;
import se.sundsvall.dept44.support.Identifier;

import static io.swagger.v3.oas.models.security.SecurityScheme.In.HEADER;
import static io.swagger.v3.oas.models.security.SecurityScheme.Type.APIKEY;
import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.caremanagement.eventlog.web.IdentifierOpenApiConfiguration.SCHEME_NAME;

class IdentifierOpenApiConfigurationTest {

	private final IdentifierOpenApiConfiguration configuration = new IdentifierOpenApiConfiguration();

	@Test
	void registersSentByAsAnApiKeyHeaderScheme() {
		final var openApi = new OpenAPI().components(new Components());

		configuration.sentByHeaderCustomizer().customise(openApi);

		final var scheme = openApi.getComponents().getSecuritySchemes().get(SCHEME_NAME);
		assertThat(scheme).isNotNull();
		assertThat(scheme.getType()).isEqualTo(APIKEY);
		assertThat(scheme.getIn()).isEqualTo(HEADER);
		assertThat(scheme.getName()).isEqualTo(Identifier.HEADER_NAME);
		assertThat(scheme.getDescription()).contains("type=adAccount", "type=partyId");
	}

	@Test
	void appliesTheSchemeToEveryOperation() {
		final var openApi = new OpenAPI().components(new Components());

		configuration.sentByHeaderCustomizer().customise(openApi);

		assertThat(openApi.getSecurity()).hasSize(1);
		assertThat(openApi.getSecurity().getFirst()).containsKey(SCHEME_NAME);
	}

	@Test
	void createsComponentsWhenTheDocumentHasNone() {
		final var openApi = new OpenAPI();

		configuration.sentByHeaderCustomizer().customise(openApi);

		assertThat(openApi.getComponents()).isNotNull();
		assertThat(openApi.getComponents().getSecuritySchemes()).containsOnlyKeys(SCHEME_NAME);
	}

	@Test
	void keepsSecuritySchemesThatAreAlreadyPresent() {
		final var openApi = new OpenAPI().components(new Components()
			.addSecuritySchemes("other", new SecurityScheme()));

		configuration.sentByHeaderCustomizer().customise(openApi);

		assertThat(openApi.getComponents().getSecuritySchemes()).containsOnlyKeys("other", SCHEME_NAME);
	}
}
