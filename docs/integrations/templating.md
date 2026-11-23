# Templating

api-service-templating. Renders a stored template to PDF.

## Used by

Nothing yet. `TemplatingIntegration` has no callers in `src/main`. See [architecture.md](../architecture.md).

TODO: confirm whether decision documents or notices are still meant to be rendered here, or whether the client can go.

## Calls

| Call | Purpose |
|---|---|
| `POST /{municipalityId}/render/pdf` | Render a template to PDF, returned BASE64-encoded |

## Authentication

OAuth2 client credentials, registration `templating`.

## Code and configuration

- Client: `src/main/java/se/sundsvall/caremanagement/templating/integration/`, models generated from
  `src/main/resources/integrations/templating-api.yml`.
- Configuration: `integration.templating` and the `templating` registration in `application.yml`.

## Behaviour worth knowing

- `TemplatingIntegration.renderPdf` decodes the BASE64 output to bytes and answers 502 when the output is empty.
- A 404 from templating stays a 404. Every other error becomes a 502 (`TemplatingConfiguration`).
- The application still needs `INTEGRATION_TEMPLATING_*` to start, although nothing renders.
