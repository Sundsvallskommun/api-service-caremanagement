# Lifecare ProfessionalWeb

The internal api2 endpoints Lifecare's own web client calls, in the module `WESE.FC.ProfessionalWeb`. The service uses
them for what the public FamilyCare API lacks: editing a normberäkning, decisions, payments and payees, reminders,
journal notes and documents, job stimulus periods, and Lifecare's PDF prints. The API is undocumented and unversioned
on Lifecare's side, so all of it stays behind `ProfessionalWebClient`.

## Used by

The financial assistance resources under `/{municipalityId}/{namespace}/errands/financial-assistance`, mostly below
`/{errandId}/lifecare`, and the finalize and payment flows. See [architecture.md](../architecture.md).

| Caller in `types/financialassistance/service/lifecare/` |                                                       Resource it backs                                                       |
|---------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------|
| `calculation/LifecareCalculationClient`                 | `FinancialAssistanceLifecareCalculationResource` (`/calculation`, `/normberakning`). Also used by `SurplusCalculationRemover` |
| `LifecareDecisionClient`                                | `FinancialAssistanceLifecareDecisionResource`, the finalize flow (`LifecareFinalizeService`) and the section status           |
| `LifecarePaymentApi`                                    | `FinancialAssistanceLifecarePaymentResource` and the payment registration (`LifecarePaymentRegistrationService`)              |
| `LifecareJobStimulusApi`                                | `FinancialAssistanceLifecareJobStimulusResource`                                                                              |
| `LifecareRecordService`                                 | `FinancialAssistanceLifecareRecordResource` (journal notes and documents)                                                     |
| `LifecareReminderService`                               | `FinancialAssistanceLifecareReminderResource`                                                                                 |
| `LifecareSectionStatusService`                          | `FinancialAssistanceLifecareSectionStatusResource`                                                                            |

## Calls

All paths are below the module `WESE.FC.ProfessionalWeb/`. Each one goes to Lifecare through
api-service-lifecare-integrator's `POST /2281/professional-web/exchange`, see
[lifecare-integrator.md](lifecare-integrator.md).

|            Area             |                                                                                                                     Operations                                                                                                                      |
|-----------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Calculation                 | `api2/Calculation/GetProposalService`, `ListCalculations`, `GetCalculation`, `GetCalculationForEdit`, `PlacePersons`, `GetAmount`, `GetSharedCost`, `GetJobStimuliForCalculation`, `Create`, `Update`, `Delete`                                     |
| Job stimulus                | `api2/Calculation/GetJobStimulusForService`, `GetJobStimulusToDate`, `SaveJobStimulus`                                                                                                                                                              |
| Decision                    | `api2/Decision/GetProposalForService`, `GetMappedDecisionReasons`, `GetDecision`, `Create`, `Update`                                                                                                                                                |
| Payment                     | `api2/Payment/GetPaymentForCreate`, `GetLatestPayments`, `Create`; `api2/Payee/Create`; `api2/Household/HasHouseholdThisDate/`                                                                                                                      |
| Journal notes and documents | `api2/Document/GetDocumentsListForClient/`, `GetJournalNoteWithContent/`, `GetDocumentWithContent/`, `GetNoteProposalForService`, `GetDocumentProposalForService`, `CreateJournalNote/`, `CreateDocument/`, `UpdateJournalNote/`, `UpdateDocument/` |
| Reminders                   | `api2/Reminders/ListRemindersByServiceId`, `GetProposalForService`, `GetReminderEditComposite`, `CreateReminder/`, `UpdateReminder/`, `RemoveReminder/`                                                                                             |
| Prints                      | `RenderPdf/PrintContainer` (calculation), `RenderPdf/PrintDecision`, `RenderPdf/PrintDocument`                                                                                                                                                      |

A write sends back the object Lifecare handed out, a proposal or the current record, with a few fields changed. The
client works on JSON trees for that reason, so fields the service does not know about survive the round trip.

## Authentication

Lifecare wants a signed-in session of an integration account, not an API key. The integrator holds the account and
the session, and does the session escalation. careM authenticates only to the integrator, with the
`lifecare-integrator` OAuth2 registration.

## Code and configuration

- Code: `src/main/java/se/sundsvall/caremanagement/lifecare/professionalweb/`, and the transport in its
  `integrator/` subpackage.
- Callers: `src/main/java/se/sundsvall/caremanagement/types/financialassistance/service/lifecare/`.
- Configuration: `integration.lifecare-professionalweb` in `application.yml` holds the test decision maker and the
  three print template ids. How Lifecare is reached is configured under `integration.lifecare-integrator`.

## Behaviour worth knowing

- **Lifecare's answer comes back as data.** The integrator returns Lifecare's own status, content type and body, so
  a 461 or a PDF reaches careM unchanged. An exchange answer without Lifecare's answer in it is a 502.
- **Error mapping** (`ProfessionalWebErrors`): 400 → 400, 403 → 403, 404 → 404, Lifecare's 461 (refused but
  understood) → 422, anything else → 502. Lifecare's sentence under `exceptionMessage` or `message` becomes the
  problem's detail on 400, 461 and 502. An answer that is not JSON where JSON was expected is a 502.
- **Prints may ask questions first.** Some templates, such as "Beslut" for a decision with a medsökande, answer with an
  HTML form (`form#myForm`) instead of a PDF. `ParameterQueryForm` reads its fields off the page, since the names vary
  per template, and `ProfessionalWebClient.getPdf` posts them back with Lifecare's defaults, as a caseworker pressing
  Klar would. A second round of questions, or an answer that is not a PDF, is a 502.
- **Nothing is logged beyond method, path and status.** Answers carry personnummer and bodies carry what the
  caseworker wrote. The paths `/**/professional-web/**` are excluded from logbook in `application.yml`.
- **No health indicator.** careM holds no session, so it has nothing of its own to report on ProfessionalWeb.
- **Its own circuit breaker.** The Feign client `ProfessionalWebIntegratorClient` has the circuit breaker
  `lifecare-integrator-professionalweb`, so it does not share one with the FamilyCare reads. The municipality id is
  fixed at 2281.
- **Payment/Create is not idempotent.** careM never sends it again. `LifecarePaymentRegistrationService` refuses a
  payment Lifecare already holds (409) instead of making it again.
- **Lifecare accesses are logged per errand** by `LifecareAccessRecorder`. A read whose access cannot be logged is not
  served; a write whose access cannot be logged stands.

