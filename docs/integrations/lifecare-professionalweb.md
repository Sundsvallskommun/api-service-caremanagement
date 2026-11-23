# Lifecare ProfessionalWeb

The internal api2 endpoints Lifecare's own web client calls, in the module `WESE.FC.ProfessionalWeb`. The service uses
them for what the public FamilyCare API lacks: editing a normberäkning, decisions, payments and payees, reminders,
journal notes and documents, job stimulus periods, and Lifecare's PDF prints. The API is undocumented and unversioned
on Lifecare's side, so all of it stays behind `ProfessionalWebClient`.

## Used by

The financial assistance resources under `/{municipalityId}/{namespace}/errands/financial-assistance`, mostly below
`/{errandId}/lifecare`, and the finalize and payment flows. See [architecture.md](../architecture.md).

| Caller in `types/financialassistance/service/lifecare/` | Resource it backs |
|---|---|
| `calculation/LifecareCalculationClient` | `FinancialAssistanceLifecareCalculationResource` (`/calculation`, `/normberakning`). Also used by `SurplusCalculationRemover` |
| `LifecareDecisionClient` | `FinancialAssistanceLifecareDecisionResource`, the finalize flow (`LifecareFinalizeService`) and the section status |
| `LifecarePaymentApi` | `FinancialAssistanceLifecarePaymentResource` and the payment registration (`LifecarePaymentRegistrationService`) |
| `LifecareJobStimulusApi` | `FinancialAssistanceLifecareJobStimulusResource` |
| `LifecareRecordService` | `FinancialAssistanceLifecareRecordResource` (journal notes and documents) |
| `LifecareReminderService` | `FinancialAssistanceLifecareReminderResource` |
| `LifecareSectionStatusService` | `FinancialAssistanceLifecareSectionStatusResource` |

## Calls

All paths are below `{url}/WESE.FC.ProfessionalWeb/`.

| Area | Operations |
|---|---|
| Calculation | `api2/Calculation/GetProposalService`, `ListCalculations`, `GetCalculation`, `GetCalculationForEdit`, `PlacePersons`, `GetAmount`, `GetSharedCost`, `GetJobStimuliForCalculation`, `Create`, `Update`, `Delete` |
| Job stimulus | `api2/Calculation/GetJobStimulusForService`, `GetJobStimulusToDate`, `SaveJobStimulus` |
| Decision | `api2/Decision/GetProposalForService`, `GetMappedDecisionReasons`, `GetDecision`, `Create`, `Update` |
| Payment | `api2/Payment/GetPaymentForCreate`, `GetLatestPayments`, `Create`; `api2/Payee/Create`; `api2/Household/HasHouseholdThisDate/` |
| Journal notes and documents | `api2/Document/GetDocumentsListForClient/`, `GetJournalNoteWithContent/`, `GetDocumentWithContent/`, `GetNoteProposalForService`, `GetDocumentProposalForService`, `CreateJournalNote/`, `CreateDocument/`, `UpdateJournalNote/`, `UpdateDocument/` |
| Reminders | `api2/Reminders/ListRemindersByServiceId`, `GetProposalForService`, `GetReminderEditComposite`, `CreateReminder/`, `UpdateReminder/`, `RemoveReminder/` |
| Prints | `RenderPdf/PrintContainer` (calculation), `RenderPdf/PrintDecision`, `RenderPdf/PrintDocument` |

A write sends back the object Lifecare handed out, a proposal or the current record, with a few fields changed. The
client works on JSON trees for that reason, so fields the service does not know about survive the round trip.

## Authentication

A signed-in session of an integration account, not an API key. `integration.lifecare-professionalweb.provider` picks
the transport:

- `direct` (the default): `DirectProfessionalWebTransport` signs in and calls Lifecare itself. This only works from
  inside the municipal network.
- `integrator`: `IntegratorProfessionalWebTransport` sends every call to api-service-lifecare-integrator's
  `POST /2281/professional-web/exchange`, and the integrator holds the account. See
  [lifecare-integrator.md](lifecare-integrator.md).

On the direct route the sign-in (`ProfessionalWebSignIn`) walks the browser flow:

1. Set the configuration cookies the identity portal would read off the query string (`metadomain`, `actor`,
   `idpmethod`, `federationprofile`).
2. Open `{url}/WE.Flow.Html` and follow the redirects by hand to MobilityGuard, carrying cookies from hop to hop.
3. Post the forms it meets, filling in username and password on the sign-in form, and post the SAML assertion back.
4. The `LEGACY-TOKEN` cookie proves the session. Each call sends the cookies, plus the token again as `X-LEGACY-TOKEN`,
   which Lifecare checks against the cookie on a write.

The sign-in cannot answer a second factor. A sign-in page asked twice, a form that posts to itself (a waiting room)
or a page with no password field ends in a 502 naming the step.

## Code and configuration

- Code: `src/main/java/se/sundsvall/caremanagement/lifecare/professionalweb/`, and the integrator route in its
  `integrator/` subpackage.
- Callers: `src/main/java/se/sundsvall/caremanagement/types/financialassistance/service/lifecare/`.
- Configuration: `integration.lifecare-professionalweb` in `application.yml`. The integrator route uses
  `integration.lifecare-integrator` instead.

## Behaviour worth knowing

- **One session per process.** `ProfessionalWebSession` keeps it in memory and shares it between all callers. Sign-in
  and module bootstrap are single-flight, and a failed sign-in is not cached. A session past `session-ttl` is replaced
  before use. A restart signs in again.
- **Session escalation on the direct route.** Lifecare answers a call that needs a session with its own status 360.
  The transport then bootstraps the module (`{module}/Heartbeat`) and resends, then signs in afresh and resends
  (bootstrapping once more if asked), then gives up with 502. Resending a write is safe because Lifecare refuses for
  the session before it acts. When several requests find the same session dead, only the first resets it.
- **Cookies rotate.** Lifecare sets new cookies on ordinary calls, and the session takes them in after every answer.
- **Error mapping** (`ProfessionalWebErrors`): 400 → 400, 403 → 403, 404 → 404, Lifecare's 461 (refused but
  understood) → 422, anything else → 502. Lifecare's sentence under `exceptionMessage` or `message` becomes the
  problem's detail on 400, 461 and 502. An answer that is not JSON where JSON was expected is a 502.
- **Prints may ask questions first.** Some templates, such as "Beslut" for a decision with a medsökande, answer with an
  HTML form (`form#myForm`) instead of a PDF. `ParameterQueryForm` reads its fields off the page, since the names vary
  per template, and `ProfessionalWebClient.getPdf` posts them back with Lifecare's defaults, as a caseworker pressing
  Klar would. A second round of questions, or an answer that is not a PDF, is a 502.
- **No automatic redirects, no Feign.** `ProfessionalWebHttp` uses the JDK `HttpClient`, pinned to HTTP/1.1, with
  redirects off and trust from the dept44 truststore. Statuses are read, not thrown.
- **Nothing is logged beyond method, path and status.** Answers carry personnummer and bodies carry what the
  caseworker wrote. The paths `/**/professional-web/**` are excluded from logbook in `application.yml`.
- **Not configured is not fatal.** With `url` unset every call answers 502 and the application still starts.
- **Health.** `ProfessionalWebHealthIndicator` (`lifecareProfessionalWeb`, direct route only) never calls Lifecare. It
  reports UP while a session is held and UNKNOWN before the first call or when `url` is unset.
- **Integrator route.** The Feign client `ProfessionalWebIntegratorClient` has its own circuit breaker,
  `lifecare-integrator-professionalweb`, so it does not share one with the FamilyCare reads. The municipality id is
  fixed at 2281. Session handling and escalation happen in the integrator.
- **Payment/Create is not idempotent.** Apart from the resend after a session refusal, nothing sends it again. `LifecarePaymentRegistrationService` refuses
  a payment Lifecare already holds (409) instead of making it again.
- **Lifecare accesses are logged per errand** by `LifecareAccessRecorder`. A read whose access cannot be logged is not
  served; a write whose access cannot be logged stands.
- TODO: which Lifecare account and environments the direct route is configured for, and who owns the integration
  account's credentials.
