# Financial aid

api-service-financial-aid, the proxy in front of SSBTEK. Returns what the agencies (af, csn, fk, skv, so, tns, miv)
know about a person's payments for a period.

## Used by

`GET /{municipalityId}/{namespace}/errands/financial-assistance/{errandId}/ssbtek`, through
`FinancialAssistanceSsbtekService`. See [architecture.md](../architecture.md).

TODO: the comment on `FinancialAssistanceSsbtekService` mentions a `fetch-financial-aid-basis` BPMN task. If the
process calls financial aid as well, that call is not in this repository.

## Calls

|                                  Call                                   |                    Purpose                     |
|-------------------------------------------------------------------------|------------------------------------------------|
| `GET /{municipalityId}/financial-aid?personalNumber=&fromDate=&toDate=` | The per-agency basis for one person and period |

## Authentication

OAuth2 client credentials, registration `financial-aid`.

## Code and configuration

- Client: `src/main/java/se/sundsvall/caremanagement/financialaid/integration/`. No generated model. The answer is an
  untyped map per agency.
- Configuration: `integration.financial-aid` and the `financial-aid` registration in `application.yml`.

## Behaviour worth knowing

- Every failure becomes a 502 with the upstream status in the detail (`FinancialAidIntegration`). The exception message
  is dropped, since a transport error carries the URL and with it the personnummer.
- Feign logging is off for this client, and nothing logs the personnummer or the answer.
- `FinancialAssistanceSsbtekService` stores nothing. It keeps only the fields in `SsbtekPaymentViewFields` and defaults
  the period to whole months, M−2 through the current month.

