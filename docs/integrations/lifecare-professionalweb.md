# Lifecare ProfessionalWeb

The internal api2 endpoints Lifecare's own web client calls, in the module `WESE.FC.ProfessionalWeb`, reached through
lifecare-integrator's resources. The service uses them for what the public FamilyCare API lacks: editing a
normberäkning, decisions, payments and payees, reminders, journal notes and documents, job stimulus periods, and
Lifecare's PDF prints. The API is undocumented and unversioned on Lifecare's side, so all of it stays behind
`ProfessionalWebClient`.

## Used by

The financial assistance resources under `/{municipalityId}/{namespace}/errands/financial-assistance`, mostly below
`/{errandId}/lifecare`, and the finalize and payment flows. See [architecture.md](../architecture.md).

|                                        Caller in `types/financialassistance/service/lifecare/`                                         |                                                                   Resource it backs                                                                   |
|----------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------|
| `calculation/ErrandLifecareCalculationService`, `calculation/ErrandNormberakningService`, `calculation/LifecareCalculationEditService` | `FinancialAssistanceLifecareCalculationResource` (`/calculation`, `/normberakning`). `LifecareCalculationClient` builds the bodies of the norm counts |
| `calculation/SurplusCalculationRemover`                                                                                                | The removal of a beräkning careM created twice                                                                                                        |
| `LifecareDecisionService`                                                                                                              | `FinancialAssistanceLifecareDecisionResource`. `LifecareDecisionClient` reads the underlag from a normberäkning                                       |
| `LifecareFinalizeService`                                                                                                              | The finalize flow                                                                                                                                     |
| `ErrandLifecarePaymentService`                                                                                                         | `FinancialAssistanceLifecarePaymentResource`                                                                                                          |
| `LifecarePaymentRegistrationService`                                                                                                   | The payment registration. `LifecarePaymentApi` holds the hushåll check                                                                                |
| `ErrandLifecareJobStimulusService`                                                                                                     | `FinancialAssistanceLifecareJobStimulusResource`                                                                                                      |
| `LifecareRecordService`                                                                                                                | `FinancialAssistanceLifecareRecordResource` (journal notes and documents)                                                                             |
| `LifecareReminderService`                                                                                                              | `FinancialAssistanceLifecareReminderResource`                                                                                                         |
| `LifecareSectionStatusService`                                                                                                         | `FinancialAssistanceLifecareSectionStatusResource`                                                                                                    |

## Calls

Each call goes to one of api-service-lifecare-integrator's resources under `/2281/professional-web/`, see
[lifecare-integrator.md](lifecare-integrator.md). The integrator makes the matching api2 or print call in Lifecare and
returns Lifecare's JSON untouched. `ProfessionalWebClient` has one method per resource.

|            Area             |                                                                                                                                          Resources                                                                                                                                          |
|-----------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Calculation                 | `GET services/{serviceId}/calculations/proposal`, `/calculations/latest`; `GET calculations/{calculationId}`, `/edit`; `POST calculations/place-persons`, `/amount`, `/shared-cost`, `/job-stimuli`; `POST services/{serviceId}/calculations`; `PUT`, `DELETE calculations/{calculationId}` |
| Job stimulus                | `GET services/{serviceId}/job-stimulus`, `GET job-stimulus/end-date?fromDate=`, `POST job-stimulus`                                                                                                                                                                                         |
| Decision                    | `GET services/{serviceId}/decisions/proposal?amountType=&calculationId=`, `GET decision-codes/{decisionCode}/reasons`, `GET decisions/{decisionId}`, `POST services/{serviceId}/decisions`, `PUT decisions/{decisionId}`                                                                    |
| Payment                     | `GET services/{serviceId}/payments/proposal`, `/payments/latest`, `/household?date=`; `POST services/{serviceId}/payments`; `POST payees`                                                                                                                                                   |
| Journal notes and documents | `GET persons/{partyId}/documents`; `GET journal-notes/{id}`, `/edit`; `GET documents/{id}`, `/edit`; `GET services/{serviceId}/journal-notes/proposal`, `/documents/proposal`; `POST journal-notes`, `POST documents`; `PUT journal-notes/{id}`, `PUT documents/{id}`                       |
| Reminders                   | `GET services/{serviceId}/reminders`, `/reminders/proposal`; `POST reminders`; `GET`, `PUT`, `DELETE reminders/{reminderId}`                                                                                                                                                                |
| Prints                      | `GET calculations/{calculationId}/pdf`, `GET decisions/{decisionId}/pdf`, `GET documents/{id}/pdf`, with `Accept: application/pdf`                                                                                                                                                          |

A write sends back the object Lifecare handed out, a proposal or the current record, with a few fields changed. The
client works on JSON trees for that reason, so fields the service does not know about survive the round trip.

## Authentication

Lifecare wants a signed-in session of an integration account, not an API key. The integrator holds the account and
the session, and does the session escalation. careM authenticates only to the integrator, with the
`lifecare-integrator` OAuth2 registration.

## Code and configuration

- Code: `src/main/java/se/sundsvall/caremanagement/lifecare/professionalweb/`, and the Feign client, its error
  decoder and its circuit breaker rule in the `integrator/` subpackage.
- Callers: `src/main/java/se/sundsvall/caremanagement/types/financialassistance/service/lifecare/`.
- Configuration: `integration.lifecare-professionalweb` in `application.yml` holds the test decision maker. How Lifecare
  is reached is configured under `integration.lifecare-integrator`. The print templates are configured in the
  integrator, not here.

## Behaviour worth knowing

- **Error mapping** (`ProfessionalWebIntegratorErrorDecoder`, `ProfessionalWebErrors`): the integrator's 400, 403,
  404 and 422 keep their status, anything else is a 502. The integrator's `detail`, which carries Lifecare's sentence
  when there is one, becomes the problem's detail. A problem without a detail gets `lifecare-integrator answered
  <status>`. A timeout, an integrator that cannot be reached or an open circuit is a 502. A 401 whose
  `WWW-Authenticate` says `invalid_token` is retryable, so dept44's OAuth2 retryer drops the token and tries once more
  with a new one.
- **On Create, only no answer is uncertain.** On Create of a decision, payment or calculation, the integrator's 400,
  403, 404 and 422 pass on with its status and detail. Any other failure (502, a timeout, an open circuit, an
  integrator that cannot be reached) answers that it is not known whether Lifecare saved it, so the caseworker checks
  before saving again.
- **A print that is not a PDF is a 502.** The integrator answers the parameter questions some templates ask, such as
  "Beslut" for a decision with a medsökande, so careM only checks that the answer starts with `%PDF`.
- **The document list is read by partyId.** careM sends the applicant's partyId and no personnummer. When the
  integrator cannot resolve the partyId, its 404 reaches the caller.
- **The household check is asked by insats.** `services/{serviceId}/household` takes the payment date; the integrator
  reads the person off the insats's payment underlag itself.
- **Nothing is logged.** Answers carry personnummer and bodies carry what the caseworker wrote. The paths
  `/**/professional-web/**` are excluded from logbook in `application.yml`.
- **No health indicator.** careM holds no session, so it has nothing of its own to report on ProfessionalWeb.
- **Its own circuit breaker.** The Feign client `ProfessionalWebIntegratorClient` has the circuit breaker
  `lifecare-integrator-professionalweb`, so it does not share one with the FamilyCare reads. Its
  `ProfessionalWebCircuitBreakerFailures` does not count 400, 403, 404 or 422 as failures, so refused writes do not
  open it. The municipality id is fixed at 2281.
- **Payment/Create is not idempotent.** careM never sends it again. `LifecarePaymentRegistrationService` refuses a
  payment Lifecare already holds (409) instead of making it again.
- **Lifecare accesses are logged per errand** by `LifecareAccessRecorder`. A read whose access cannot be logged is not
  served; a write whose access cannot be logged stands.

