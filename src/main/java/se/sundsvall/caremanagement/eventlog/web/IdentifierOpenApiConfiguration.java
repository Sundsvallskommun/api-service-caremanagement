package se.sundsvall.caremanagement.eventlog.web;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.sundsvall.dept44.support.Identifier;

import static io.swagger.v3.oas.models.security.SecurityScheme.In.HEADER;
import static io.swagger.v3.oas.models.security.SecurityScheme.Type.APIKEY;

/**
 * Makes the {@code X-Sent-By} header that {@link RequireIdentifierInterceptor} demands visible — and fillable — in
 * Swagger UI.
 *
 * <p>
 * The interceptor rejects every municipality-scoped request without a parseable {@link Identifier}, but the header is
 * not part of any operation's signature, so "Try it out" had no field for it and 400'd on the whole business surface.
 * Registering it as an {@code apiKey} security scheme and applying it globally gives Swagger UI an "Authorize" button:
 * the value is typed once and sent on every subsequent request.
 *
 * <p>
 * It is a security scheme only in the OpenAPI sense — the header is an identity claim for the errand event log, not
 * authentication. Declaring it per-operation with {@code @Parameter}/{@code @RequestHeader} instead would make Swagger
 * UI send it twice (its own field plus the global one), and the comma-joined value that produces is not parseable by
 * {@link Identifier#parse(String)}, so operations read the identity from {@link Identifier#get()} rather than binding
 * the header themselves.
 */
@Configuration
class IdentifierOpenApiConfiguration {

	static final String SCHEME_NAME = "sentBy";

	@Bean
	OpenApiCustomizer sentByHeaderCustomizer() {
		return openApi -> {
			if (openApi.getComponents() == null) {
				openApi.setComponents(new Components());
			}
			openApi.getComponents().addSecuritySchemes(SCHEME_NAME, new SecurityScheme()
				.type(APIKEY)
				.in(HEADER)
				.name(Identifier.HEADER_NAME)
				.description("Caller identity, e.g. 'joe001doe; type=adAccount' or '<uuid>; type=partyId'. Not authentication - it is the actor recorded in the errand event log."));
			openApi.addSecurityItem(new SecurityRequirement().addList(SCHEME_NAME));
		};
	}
}
