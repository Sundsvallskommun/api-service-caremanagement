# Lifecare FamilyCare

Tieto's FamilyCare API (`WESE.FC.Api.FC`), Lifecare's API for individual and family care. careM reads a person's
case history from it and writes a financial assistance errand back as an actualisation, a calculation and attached
PDFs.

careM never calls FamilyCare itself. Every call goes through api-service-lifecare-integrator, see
[lifecare-integrator.md](lifecare-integrator.md). `LifecareIntegratorIntegration` is the one implementation of
`LifecareFamilyCare`, the contract the services call. This file describes that contract.

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

The integrator's resources, one per contract method, are listed in [lifecare-integrator.md](lifecare-integrator.md).

The contract answers with FamilyCare's own model, generated from
`src/main/resources/integrations/lifecare-familycare-api.json` into `generated.se.sundsvall.lifecarefamilycare`. The
integrator's answers are mapped back into it, so the services work on FamilyCare's types.

## Authentication

careM holds no FamilyCare key. It authenticates to the integrator, which holds FamilyCare's domain and keys.

## Code and configuration

- Contract: `src/main/java/se/sundsvall/caremanagement/lifecare/integration/LifecareFamilyCare.java`.
- Implementation: `lifecare/integration/integrator/`.
- Configuration: `integration.lifecare-familycare` in `application.yml` holds only the lookback windows, the open
  actualisation statuses and the caseworker directory limit. How Lifecare is reached is configured under
  `integration.lifecare-integrator`.

## Behaviour worth knowing

- **Arguments and responses carry partyIds.** No personnummer crosses the contract in either direction, so
  `LifecareCaseService` passes the party ids in a response on without a citizen lookup.
- **Every failure becomes 502.** `LifecareIntegratorIntegration.call` wraps anything thrown in a `BAD_GATEWAY` problem
  that names the action and the upstream status. For a failure that is not already a problem only the exception class
  is kept, since a transport error carries the request URL with the partyId.
- **An unknown person is `null`, not an error.** `getPerson` answers `null` when Lifecare holds no such person, which
  is every first-time applicant. See [lifecare-integrator.md](lifecare-integrator.md) for which 404 that is.
- **Dates on a write go out with a time.** `FamilyCareDates` renders dates as `T00:00:00` when the assemblers build an
  actualisation or a calculation. The integrator takes the date part.
