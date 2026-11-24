# Operaton

The Operaton BPMN engine (api-operaton). Runs each errand type's process and evaluates the DMN tables that hold the
financial assistance rules.

## Used by

All calls go through `ProcessService`, which any module may use (`operaton` is a Modulith shared module). See
[architecture.md](../architecture.md).

|                                                      Flow                                                      |                                                                    Caller                                                                     |
|----------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------|
| Start a financial assistance errand's process when the errand is created                                       | `FinancialAssistanceProcessStarter`                                                                                                           |
| Start the process again for errands whose start failed                                                         | `FinancialAssistanceProcessStartRetryWorker`                                                                                                  |
| End the process when an errand is withdrawn (`ErrandWithdrawn`)                                                | `FinancialAssistanceWithdrawalListener`                                                                                                       |
| Resume the process after a decision is finalized                                                               | `FinancialAssistanceFinalizeService`                                                                                                          |
| Send a message on a client's request, `POST /{municipalityId}/{namespace}/errands/{errandId}/process-messages` | `ProcessMessageResource`                                                                                                                      |
| Resend queued messages                                                                                         | `ProcessMessageRetryWorker`                                                                                                                   |
| Evaluate rule tables                                                                                           | `ApplicationRulesService`, `ExpenseRulesService`, `PeriodRulesService`, `IncomeChangeFeeder`, `RenewalDeltaService`, `DefaultAssigneeService` |

## Calls

|                            Call                             |                                Purpose                                 |
|-------------------------------------------------------------|------------------------------------------------------------------------|
| `GET /{municipalityId}/process-definitions?name=`           | Resolve a definition name to its key before a start or a running check |
| `POST /{municipalityId}/process-instances`                  | Start a process, business key = errandId                               |
| `GET /{municipalityId}/process-instances`                   | List active instances, to see whether an errand already has one        |
| `POST /{municipalityId}/message`                            | Correlate a message by business key                                    |
| `POST /{municipalityId}/decision-definitions/evaluate?key=` | Evaluate a DMN decision, keys `Decision_*` in the rule services        |

`OperatonClient` also declares `GET /process-instances/{id}` and `POST /process-instances/{id}/variables`. Nothing
calls them.

The process definitions started are `rakel-ekonomiskt-bistand`, `rakel-ekonomiskt-bistand-tillaggsansokan` and
`rakel-ekonomiskt-bistand-nyansokan`, chosen by the errand's type slug in `FinancialAssistanceProcessStarter`. The BPMN
and DMN files are not in this repository. TODO: link to where they are kept and deployed.

## Authentication

OAuth2 client credentials, registration `operaton`. TODO: `application.yml` sets neither a client id, a secret nor a
token URI for this registration, and no `integration.operaton.url`. Find out how each environment supplies them.

## Code and configuration

- Client: `src/main/java/se/sundsvall/caremanagement/operaton/integration/`, models generated from
  `src/main/resources/integrations/api-operaton.yaml`.
- Service: `src/main/java/se/sundsvall/caremanagement/operaton/service/ProcessService.java`.
- Configuration: `integration.operaton`, the `operaton` circuit breaker and `scheduler.process-message-retry` in
  `application.yml`.

## Behaviour worth knowing

- The engine's 404 stays a 404. Every other error becomes a 502 (`OperatonConfiguration`). A 404 on a correlation
  means no instance waits for the message. `OperatonCircuitBreakerFailures` keeps it from counting against the circuit
  breaker.
- A correlation checks that the errand exists in the caller's municipality and namespace first (`ErrandAccessGuard`).
- A message that could not be correlated can be queued in `process_message_retry`, in the same transaction as what it
  reports. `ProcessMessageRetryWorker` resends due rows with backoff of 1, 2, 4 minutes up to an hour, drops a row when
  the errand has no running process, and gives up after 3 days with an error log.
- For `ErrandWithdrawn` a 404 counts as done, since the process has already ended (`ProcessService.isNothingToEnd`).
- The active-instance list has no business-key filter. `ProcessService` filters the whole list itself, and suspended
  or ended instances do not count as running.
- An unknown definition name is a 400 from `ProcessService`. A process start at errand creation logs and swallows
  failures, and the scheduled retry picks the errand up later.

