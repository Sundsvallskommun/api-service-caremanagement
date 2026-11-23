# Lifecare FamilyCare

Tieto's FamilyCare API (`WESE.FC.Api.FC`), Lifecare's API for individual and family care. careM reads a person's
case history from it and writes a financial assistance errand back as an actualisation, a calculation and attached
PDFs.

FamilyCare has two routes, and `integration.lifecare-familycare.provider` picks one at startup:

| Provider               | Bean                                                    | Route                                                                   |
|------------------------|---------------------------------------------------------|-------------------------------------------------------------------------|
| `familycare` (default) | `LifecareFamilyCareIntegration`                         | Straight to FamilyCare. Only works inside the municipal network         |
| `integrator`           | `LifecareIntegratorIntegration`                         | Through api-service-lifecare-integrator, see [lifecare-integrator.md](lifecare-integrator.md) |

Both implement `LifecareFamilyCare`, so the services never know which route is wired. This file describes the direct
route and the contract both share.

## Used by

Only the services in `src/main/java/se/sundsvall/caremanagement/lifecare/service/` call it. They back the financial
assistance flows in `types/financialassistance`, see [architecture.md](../architecture.md):

| Service                      | Calls                                                                     | Used for                                                                  |
|------------------------------|---------------------------------------------------------------------------|---------------------------------------------------------------------------|
| `LifecareCaseService`        | `getPerson`, `getActualisations`, `getDecisions`, `getCalculations`       | Eligibility, the protected-identity gate, renewal prefill, the household   |
| `LifecareCaseHistoryService` | `getCalculations`, `getDecisions`, `getPayments`, `getDocuments`, `getDocumentContent` | Decision proposal, proposal basis, Lifecare records, calculation sync |
| `ActualisationService`       | `getActualisationProposal`, `createActualisation`, `getActualisations`, `postActualisationAttachment` | The intake into Lifecare and the nightly message archive upload |
| `CalculationService`         | `getCalculationProposal`, `createCalculation`                             | Writing the calculation (normberäkning) back                              |
| `CaseworkerResolver`         | `getServices`, `getUsers`                                                 | Finding the caseworker of the applicant's previous Lifecare service       |
| `PaymentStatusService`       | `getPayments`                                                             | Payment status of an errand                                               |

## Calls

All under `/apifc/v1`, declared in `LifecareFamilyCareClient`:

| Call                                            | Purpose                                                      |
|-------------------------------------------------|--------------------------------------------------------------|
| `GET Persons`, `GET Contacts`                   | A person's master data and contacts                          |
| `GET Actualisations`, `Calculations`, `Decisions`, `Payments`, `Services`, `Documents` | A person's records in a date window |
| `GET Investigations`, `Executions`, `ResourceAllocations` | Declared, called from nowhere                     |
| `GET DocumentContent`                           | The PDF of one document                                      |
| `GET Users/GetUsers`                            | The caseworker directory                                     |
| `GET Actualisations/Proposals`                  | Code lists for building an actualisation                     |
| `POST Actualisations`                           | Create an actualisation, answers its id                      |
| `GET Calculations/Proposals`                    | Norms, household and code lists for building a calculation   |
| `POST Calculations`                             | Create a calculation, answers its id                         |
| `POST Actualisations/{id}/Attachments`          | Upload a PDF onto an actualisation (multipart)               |

The model is generated from `src/main/resources/integrations/lifecare-familycare-api.json` into
`generated.se.sundsvall.lifecarefamilycare`. Both routes answer with these generated types.

## Authentication

A `domain` and an API `key`, sent as query parameters on every request, with the key also sent as `X-API-Key`.
Requests to `/Users` use the separate `user-key` when one is set, otherwise `key`. See
`LifecareFamilyCareConfiguration`.

## Code and configuration

- Contract: `src/main/java/se/sundsvall/caremanagement/lifecare/integration/LifecareFamilyCare.java`.
- Direct route: `LifecareFamilyCareClient` and `LifecareFamilyCareIntegration` in the same package, configuration in
  `lifecare/integration/configuration/`.
- Configuration: `integration.lifecare-familycare` in `application.yml`.

## Behaviour worth knowing

- **Arguments are partyIds on both routes.** The direct route turns each one into a personnummer through
  [citizen](citizen.md) before it calls FamilyCare. A partyId the citizen service cannot resolve answers 404. On a
  calculation every household row is resolved too, and a row that does not resolve fails the whole calculation with
  502.
- **Responses differ by route.** The direct route hands back FamilyCare's personnummer in `personId` fields, the
  integrator route a partyId. `respondsWithPartyId()` says which, and `LifecareCaseService.toPartyId` uses it.
- **Every failure becomes 502.** `LifecareFamilyCareIntegration.call` wraps anything thrown in a `BAD_GATEWAY` problem
  that names the action and the upstream status. For a failure that is not already a problem only the exception class
  is kept, since a transport error carries the request URL with the personnummer and the key.
- **An unknown person is `null`, not an error.** `getPerson` answers `null` when FamilyCare says 404, which is every
  first-time applicant. dept44's decoder turns a 404 into a 502 problem, so `UpstreamNotFound` reads the status out of
  the problem's detail text.
- **Dates go out with a time.** FamilyCare rejects a bare `yyyy-MM-dd`. `FamilyCareDates` renders the window start as
  `T00:00:00` and the end as `T23:59:59`, so both ends are inclusive.
- **Nothing is logged.** Feign logging is pinned to `NONE` in `LifecareFamilyCareConfiguration`, since the query
  string carries the personnummer and the key.
- The direct client is a `@FeignClient` registered on both routes. TODO: verify whether the integrator route still
  needs `integration.lifecare-familycare.url`, `domain` and `key` set to start, as the client and the validated
  `LifecareFamilyCareProperties` suggest.
- TODO: which environments run which provider.
