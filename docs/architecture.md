# Architecture

CareManagement (careM) holds errands for social care. Each errand belongs to a municipality and a namespace and has
a type. The only type so far is financial assistance (ekonomiskt bistånd), in its three variants new, renewal and
supplementary application. careM keeps the errand, its conversation, notes, attachments and audit trail in its own
database, runs each errand's process in Operaton, and reads and writes the case in Lifecare.

TODO: list the services and frontends that call this one. The code names Draken as the caseworker interface and the
citizen portal as the place outbound messages are shown, but neither is named as a caller.

## Modules

careM is a Spring Modulith application. Each top-level package under `src/main/java/se/sundsvall/caremanagement/` is a
module with its own `api`, `service` and `integration` layers, described in its `package-info.java`. `core`, `shared`
and `operaton` are shared modules that every module may use, see `Application.java`. Type modules live under `types/`.

|                                    Module                                    |                                                                          Role                                                                          |
|------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------|
| `core`                                                                       | The errand envelope: create, read, search, patch, delete, the errand type registry and the errand events                                               |
| `types/financialassistance`                                                  | The financial assistance type: its data, eligibility routing, calculation draft, warnings, finalising, and every Lifecare read and write for an errand |
| `stakeholders`                                                               | People on an errand, with roles contributed per type                                                                                                   |
| `conversation`                                                               | Messages between caseworker and applicant, with attachments and read state                                                                             |
| `attachments`                                                                | Files on an errand                                                                                                                                     |
| `notes`                                                                      | Free-text notes on an errand                                                                                                                           |
| `decisions`                                                                  | The errand's decision trail                                                                                                                            |
| `permit`, `referral`                                                         | Permits issued on an errand, and referrals to other authorities                                                                                        |
| `cocaseworkers`                                                              | Co-caseworkers besides the assignee                                                                                                                    |
| `notifications`                                                              | Notifications to caseworkers, raised from events                                                                                                       |
| `statushistory`                                                              | Every status change, recorded from `ErrandStatusChanged`                                                                                               |
| `eventlog`                                                                   | Who did what on which errand, for every errand-scoped request and every Lifecare access                                                                |
| `formsnapshot`                                                               | The application form exactly as the citizen saw and answered it, stored once per errand                                                                |
| `errandtypes`                                                                | The catalogue of errand types and their form fields                                                                                                    |
| `metadata`, `namespaceconfig`, `document`, `journal`                         | Lookup values, per-namespace settings, and the catalogues of Lifecare document and journal types                                                       |
| `statistics`                                                                 | Errand counts per status and assignee                                                                                                                  |
| `usersettings`                                                               | Per-user preferences for the caseworker interface                                                                                                      |
| `operaton`, `citizen`, `messaging`, `templating`, `financialaid`, `lifecare` | Integrations, see [integrations/](integrations/)                                                                                                       |
| `shared`                                                                     | Auditing, the errand access guard and other types any module uses                                                                                      |

Tables and migrations: [data-model.md](data-model.md).

## Cross-cutting rules

- **Every business request names its actor.** A request under `/{municipalityId}/**` without a valid `X-Sent-By`
  header is refused with 400, see `eventlog/web/RequireIdentifierInterceptor.java`. `caremanagement.identifier.required`
  turns this off. CORS preflight is exempt.
- **Every errand-scoped request is logged.** `eventlog/web/ErrandEventInterceptor.java` writes one row per request
  under `/{municipalityId}/{namespace}/errands/{errandId}/…` and `…/errands/financial-assistance/{errandId}/…`, reads
  included. A search writes one row per errand on the returned page. Counts, the unread count, mark-as-read and reads
  of the event log itself are not logged. A failed log write never fails the request.
- **Lifecare accesses are logged on the errand.** A Lifecare read that cannot be logged is not served. A Lifecare
  write that cannot be logged stands, and the failure is logged. See
  `types/financialassistance/service/lifecare/LifecareAccessRecorder.java`.
- **A sub-resource checks its errand first.** Modules call `shared/ErrandAccessGuard.java`, which answers 404 when the
  errand does not exist in the caller's municipality and namespace. Each module's own repository query then scopes
  the child row by errand.
- **Modules talk through events.** `core` publishes `ErrandCreated`, `ErrandStatusChanged`, `ErrandAssigned` and
  `ErrandDeleted` (`core/service/event/`). Other modules react with `@ApplicationModuleListener`, which runs after the
  commit in its own transaction, and Spring Modulith stores each event until a listener has handled it.
- **Deleting an errand removes its rows in two ways.** Modules whose tables have an `ON DELETE CASCADE` foreign key
  to `errand` go with it. `attachments`, `decisions`, `permit`, `referral` and `stakeholders` delete their rows from an
  `ErrandDeleted` listener. The event log has no foreign key and keeps its rows.
- **A type sets its own statuses.** `core` accepts only the statuses and transitions the type's
  `ErrandTypeContribution` allows, see `core/service/registry/`. Financial assistance registers its lifecycle in
  `types/financialassistance/configuration/FinancialAssistanceModuleConfig.java`.
- **ProfessionalWeb traffic is never logged.** Its paths are excluded from logbook in `application.yml`, since the
  bodies carry personnummer and free text.

## Flows

### A financial assistance errand is created

1. A client posts to `POST /{municipalityId}/{namespace}/errands/{typeSlug}` with one of the three financial
   assistance slugs. `FinancialAssistanceErrandService` stores the errand, its data, stakeholders, attachments and
   form snapshot, and `core` publishes `ErrandCreated`.
2. `FinancialAssistanceErrandCreatedListener` picks it up after the commit. An errand without an assignee gets the
   default caseworker.
3. The errand is frozen as `NEEDS_MANUAL_REVIEW` when the applicant, co-applicant or a child has protected identity,
   or the flag could not be read (`ProtectedIdentityGate`, reading [Citizen](integrations/citizen.md) and
   [Lifecare FamilyCare](integrations/lifecare-familycare.md)). The recently-closed guard can freeze it too, and is
   switched off by default.
4. Otherwise the errand's process is started in [Operaton](integrations/operaton.md), one process per application
   type. `FinancialAssistanceProcessStartRetryScheduler` starts it again later if the first start failed.
5. A caseworker releases a frozen errand, which starts its process then (`FinancialAssistanceReleaseListener`).

The process calls back through `POST …/errands/{errandId}/process-messages`. A message that cannot be delivered is
queued in `process_message_retry` and retried by `ProcessMessageRetryScheduler` until it is delivered or too old, see
`operaton/service/scheduler/processmessageretry/ProcessMessageRetryWorker.java`.

### A closed errand's conversation is archived

`MessageArchiveScheduler` runs nightly. For each closed financial assistance errand that has waited long enough, it
renders the conversation and its attachments into one PDF, uploads it to the errand's Lifecare actualisation, and
then stores it on the errand as a `MESSAGE_HISTORY` attachment. That attachment marks the errand as done, so a failed
upload is tried again the next night. See `types/financialassistance/archive/MessageArchiveService.java`.

### Scheduled jobs

|           Job           |                                            Class                                             |                        Does                        |
|-------------------------|----------------------------------------------------------------------------------------------|----------------------------------------------------|
| `process-message-retry` | `operaton/service/scheduler/processmessageretry/ProcessMessageRetryScheduler.java`           | Delivers queued process messages                   |
| `process-start-retry`   | `types/financialassistance/service/event/FinancialAssistanceProcessStartRetryScheduler.java` | Starts processes whose first start failed          |
| `notification-cleanup`  | `notifications/service/scheduler/NotificationCleanupScheduler.java`                          | Removes expired notifications                      |
| `message-archive`       | `types/financialassistance/archive/MessageArchiveScheduler.java`                             | Archives closed errands' conversations to Lifecare |

All are `@Dept44Scheduled` with ShedLock, so one instance runs each job at a time.

## Endpoints and their sources

All paths are under `/{municipalityId}/{namespace}`, except user settings, which is under `/{municipalityId}`. The
financial assistance paths are under `/errands/financial-assistance`, shortened to `…/fa` below. "Database" means the
module's own tables.

|                                         Endpoint                                         |                                         Resource                                          |                                                Source                                                |
|------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------|
| `/errands`, `/errands/{errandId}`, `/errands/count`                                      | `core/api/ErrandResource.java`                                                            | Database                                                                                             |
| `/errands/{errandId}/attachments/**`                                                     | `attachments/api/AttachmentResource.java`                                                 | Database                                                                                             |
| `/errands/{errandId}/co-caseworkers/**`                                                  | `cocaseworkers/api/CoCaseworkerResource.java`                                             | Database                                                                                             |
| `/errands/{errandId}/messages/**`                                                        | `conversation/api/MessageResource.java`                                                   | Database                                                                                             |
| `/errands/{errandId}/decisions/**`                                                       | `decisions/api/DecisionResource.java`                                                     | Database                                                                                             |
| `/errands/{errandId}/notes/**`                                                           | `notes/api/NoteResource.java`                                                             | Database                                                                                             |
| `/errands/{errandId}/notifications/**`, `/notifications`                                 | `notifications/api/NotificationResource.java`                                             | Database                                                                                             |
| `/errands/{errandId}/permits/**`                                                         | `permit/api/PermitResource.java`                                                          | Database                                                                                             |
| `/errands/{errandId}/referrals/**`                                                       | `referral/api/ReferralResource.java`                                                      | Database                                                                                             |
| `/errands/{errandId}/stakeholders/**`                                                    | `stakeholders/api/StakeholderResource.java`                                               | Database                                                                                             |
| `/errands/{errandId}/status-history`                                                     | `statushistory/api/StatusHistoryResource.java`                                            | Database                                                                                             |
| `/errands/{errandId}/events/**`, `/events`                                               | `eventlog/api/ErrandEventResource.java`, `ActorEventResource.java`                        | Database                                                                                             |
| `/errands/{errandId}/process-messages`                                                   | `operaton/api/ProcessMessageResource.java`                                                | [Operaton](integrations/operaton.md) message correlation                                             |
| `/errands/documents/metadata`, `/errands/journal-entries/metadata`                       | `document/api/`, `journal/api/`                                                           | Database lookups, with a built-in fallback                                                           |
| `/errand-types/**`                                                                       | `errandtypes/api/ErrandTypeResource.java`                                                 | The type registries in memory                                                                        |
| `/metadata/**`                                                                           | `metadata/api/MetadataResource.java`                                                      | Database                                                                                             |
| `/namespace-config`                                                                      | `namespaceconfig/api/NamespaceConfigResource.java`                                        | Database                                                                                             |
| `/statistics`                                                                            | `statistics/api/StatisticsResource.java`                                                  | Database                                                                                             |
| `/user-settings/{adAccount}`                                                             | `usersettings/api/UserSettingsResource.java`                                              | Database                                                                                             |
| `POST /errands/{typeSlug}`, `…/fa/{errandId}`, `…/form-snapshot`, `…/data`               | `FinancialAssistanceErrandResource.java`                                                  | Database. Creating starts the flow above                                                             |
| `…/fa/eligibility`, `…/fa/prefill`                                                       | `FinancialAssistanceIntakeResource.java`                                                  | [Citizen](integrations/citizen.md) and [FamilyCare](integrations/lifecare-familycare.md)             |
| `…/fa/actualisation`, `…/fa/actualisations/**`                                           | `FinancialAssistanceIntakeResource.java`                                                  | [FamilyCare](integrations/lifecare-familycare.md) actualisations: create, list, upload an attachment |
| `…/fa/metadata`                                                                          | `FinancialAssistanceIntakeResource.java`                                                  | Constants in `FinancialAssistanceTypes.java`                                                         |
| `…/fa/calculation/prepare`, `…/fa/{errandId}/calculation/draft/**`                       | `FinancialAssistanceCalculationResource.java`, `FinancialAssistanceDraftRowResource.java` | Database draft, seeded from FamilyCare and Citizen                                                   |
| `…/fa/{errandId}/calculation/ssbtek-changes/**`                                          | `FinancialAssistanceCalculationSyncResource.java`                                         | Database, against FamilyCare history                                                                 |
| `…/fa/{errandId}/decision-proposal`                                                      | `FinancialAssistanceProposalResource.java`                                                | FamilyCare decisions and calculations, and the database                                              |
| `…/fa/payment-status`                                                                    | `FinancialAssistancePaymentResource.java`                                                 | FamilyCare payments                                                                                  |
| `…/fa/{errandId}/ssbtek`                                                                 | `FinancialAssistanceSsbtekResource.java`                                                  | [Financial aid](integrations/financial-aid.md) (SSBTEK) and Citizen                                  |
| `…/fa/{errandId}/household-identifiers`                                                  | `HouseholdIdentifiersResource.java`                                                       | Citizen                                                                                              |
| `…/fa/{errandId}/warnings/**`                                                            | `FinancialAssistanceWarningResource.java`                                                 | Database                                                                                             |
| `…/fa/{errandId}/finalize`                                                               | `FinancialAssistanceFinalizeResource.java`                                                | [ProfessionalWeb](integrations/lifecare-professionalweb.md) decision, then Operaton                  |
| `…/fa/{errandId}/lifecare/calculation/**`, `…/lifecare/normberakning/**`                 | `FinancialAssistanceLifecareCalculationResource.java`                                     | ProfessionalWeb calculations                                                                         |
| `…/fa/{errandId}/lifecare/decision/**`, `…/fa/lifecare/decision-types/**`                | `FinancialAssistanceLifecareDecisionResource.java`                                        | ProfessionalWeb decisions                                                                            |
| `…/fa/{errandId}/lifecare/job-stimulus-periods/**`                                       | `FinancialAssistanceLifecareJobStimulusResource.java`                                     | ProfessionalWeb job stimulus                                                                         |
| `…/fa/{errandId}/lifecare/payment-options`, `…/payment-status`, `…/payments`, `…/payees` | `FinancialAssistanceLifecarePaymentResource.java`                                         | ProfessionalWeb payments and payees                                                                  |
| `…/fa/{errandId}/lifecare/documents/**`, `…/journal-note-bodies`, `…/document-bodies`    | `FinancialAssistanceLifecareRecordResource.java`                                          | ProfessionalWeb journal notes and documents                                                          |
| `…/fa/{errandId}/lifecare/reminders/**`                                                  | `FinancialAssistanceLifecareReminderResource.java`                                        | ProfessionalWeb reminders                                                                            |
| `…/fa/{errandId}/lifecare/section-status`                                                | `FinancialAssistanceLifecareSectionStatusResource.java`                                   | ProfessionalWeb                                                                                      |

The financial assistance resources are in `types/financialassistance/api/`. Every `…/fa/{errandId}/lifecare/…`
endpoint takes its Lifecare keys from the errand through `LifecareErrandService`, never from the caller. Citizen is
asked for personnummer in two places: `NormberakningDraftReader` resolves those of the draft's persons for
`…/lifecare/calculation/**` and `…/lifecare/normberakning/**`, which match Lifecare's household members on them, and
a new reminder resolves the applicant's, which Lifecare wants in the reminder's body.
