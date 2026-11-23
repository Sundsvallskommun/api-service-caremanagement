# Lifecare Integrator

api-service-lifecare-integrator. It runs inside the municipal network and reaches Lifecare for careM when careM runs
outside it. careM uses it two ways, each switched on separately:

| Use                    | Switched on by                                   | Client                              |
|------------------------|--------------------------------------------------|-------------------------------------|
| FamilyCare reads and writes | `integration.lifecare-familycare.provider=integrator` | `LifecareIntegratorClient`     |
| ProfessionalWeb exchange    | `integration.lifecare-professionalweb.provider=integrator` | `ProfessionalWebIntegratorClient` |

Both share `integration.lifecare-integrator.url` and the `lifecare-integrator` OAuth2 registration, but each client has
its own circuit breaker (`lifecare-integrator` and `lifecare-integrator-professionalweb`).

## Used by

- FamilyCare route: everything that uses [Lifecare FamilyCare](lifecare-familycare.md) when its provider is
  `integrator`. `LifecareIntegratorIntegration` implements the same `LifecareFamilyCare` contract as the direct route.
- ProfessionalWeb route: every ProfessionalWeb call when its provider is `integrator`.
  `IntegratorProfessionalWebTransport` implements `ProfessionalWebTransport`, see
  [Lifecare ProfessionalWeb](lifecare-professionalweb.md).

See [architecture.md](../architecture.md) for the endpoints behind them.

## Calls

FamilyCare route, all under `/{municipalityId}`:

| Call                                                       | Stands in for FamilyCare's                  |
|------------------------------------------------------------|---------------------------------------------|
| `GET /person`, `GET /contacts`                             | `Persons`, `Contacts`                       |
| `GET /actualisations`, `/calculations`, `/decisions`, `/payments`, `/services`, `/documents` | The period reads |
| `GET /documents/{documentId}/content`                      | `DocumentContent`                           |
| `GET /users`                                               | `Users/GetUsers`                            |
| `GET /actualisations/proposal`, `GET /calculations/proposal` | The two proposals                         |
| `POST /actualisations`, `POST /calculations`               | The two creates, answering the created id   |
| `POST /actualisations/{actualisationId}/attachments`       | The attachment upload                       |

ProfessionalWeb route: `POST /2281/professional-web/exchange`, one call per ProfessionalWeb request. careM sends the
method, path, query parameters and a JSON body or form fields. The integrator answers with Lifecare's status, content
type and the body in base64.

The FamilyCare route's model is generated from `src/main/resources/integrations/lifecare-integrator-api.yaml` into
`generated.se.sundsvall.lifecareintegrator`. The exchange records in `lifecare/professionalweb/integrator/` are
written by hand.

## Authentication

OAuth2 client credentials, registration `lifecare-integrator`. See `LifecareIntegratorConfiguration`, which both
clients use.

## Code and configuration

- FamilyCare route: `src/main/java/se/sundsvall/caremanagement/lifecare/integration/integrator/`.
- ProfessionalWeb route: `src/main/java/se/sundsvall/caremanagement/lifecare/professionalweb/integrator/`.
- Configuration: `integration.lifecare-integrator` in `application.yml`, plus the `lifecare-integrator` registration
  under `spring.security.oauth2.client`.

## Behaviour worth knowing

- **The integrator takes partyIds.** Arguments pass straight through, and responses carry partyIds, so
  `respondsWithPartyId()` is `true` on this route. careM never hands it a personnummer.
- **Responses are mapped back to FamilyCare's types.** The `Integrator*Mapper` classes turn the integrator's model
  into the generated FamilyCare DTOs, so nothing above the integration changes with the route. Amounts lose
  `BigDecimal` precision on the way, and an id that is not a number becomes `null` rather than failing the page. See
  `IntegratorValues`.
- **Investigations, executions and resource allocations answer 501.** They are not ported, since nothing calls them.
- **Creates are checked before they are sent.** A calculation without `normId` or its dates, or an actualisation
  without `date` or `typeId`, answers 400 naming the missing fields instead of reaching the integrator.
- **Failures become 502** with the action and the upstream status, as on the direct route.
- **The integrator's explanation is kept.** `BufferingProblemErrorDecoder` buffers the error body before dept44's
  decoder reads it, which it does twice. Without it the detail came back as `Unknown error`.
- **An unknown person is `null` only for the right 404.** `getPerson` answers `null` when the integrator says
  `No person found for partyId`. Its other 404, when Party cannot resolve the partyId, stays a failure.
- **The ProfessionalWeb exchange always names municipality 2281.** It is hard-coded in
  `IntegratorProfessionalWebTransport`. On this route careM holds no Lifecare session, and the ProfessionalWeb health
  indicator is not registered.
- **Nothing is logged.** Feign logging is pinned to `NONE` for both clients.
- TODO: which environments run either route through the integrator.
