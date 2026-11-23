# Cleanup

Paths are under `src/main/java/se/sundsvall/caremanagement/`.

## Code that points at documents that do not exist

`types/README.md` points at `docs/architecture/proposal-modulith.md` and
`migration-plan.md`. The Javadoc of `lifecare/professionalweb/ParameterQueryForm.java` cites
`architecture/lifecare-beslutsutskrift-parametrar.md`. None of them exist in this repo, and pointing from code into
`docs/` breaks the one-way rule anyway.

Done when both say what they need in their own words and name no docs file.

## Comments that point at another repository

`financialaid/integration/FinancialAidClient.java`, `financialaid/integration/FinancialAidIntegration.java`,
`lifecare/integration/LifecareFamilyCareClient.java` and `lifecare/integration/LifecareFamilyCareIntegration.java`
point at files in `vof-ekonomiskt-bistand`. `types/financialassistance/service/FinancialAssistanceSsbtekService.java`
points at `docs/ssbtek-regelverk.txt in the sprint workspace`. A reader of this repo may not be able to open them.

Done when each comment states the fact it relies on, or the fact moves into `docs/`.

## Comments that no longer match the code

- `lifecare/integration/integrator/configuration/LifecareIntegratorProperties.java` calls its read timeout "more
  generous than the five-second default used elsewhere". The other read timeouts are 30 s, the record defaults to
  60 s and `application.yml` sets 90 s.
- `types/financialassistance/service/lifecare/LifecarePaymentRegistrationService.java` says Payment/Create "is never
  retried". `DirectProfessionalWebTransport` sends it again after a session refusal (360).
- `core/integration/db/model/ErrandEntity.java` says each module has "a plain `errand_id` FK". `stakeholder`,
  `decision`, `attachment`, `permit`, `referral` and `errand_event` have an index on `errand_id` but no foreign key.
- `operaton/package-info.java` says type modules "terminate a BPMN flow" through it. There is no terminate call; a
  flow ends on the `ErrandWithdrawn` message.
- `messaging/package-info.java` and `templating/package-info.java` say type modules use them. None do, see
  [decisions-needed.md](decisions-needed.md).

Done when each comment says what the code does.

## Unused client operations

`operaton/integration/OperatonClient.java` has `getProcessInstance` and `modifyProcessInstanceVariables`, and
`lifecare/integration/LifecareFamilyCare.java` has `getContacts`, `getInvestigations`, `getExecutions` and
`getResourceAllocations`. Nothing in `src/main` calls them. On the integrator route the last three answer 501.

Done when they are removed, or a caller uses them.

## The root README is the dept44 template

`README.md` still has the template's placeholders (`YOUR-PROJECT-ID`, "Service Name", a MySQL datasource example) and
describes no real configuration. `AGENTS.md` sends configuration changes there.

Done when the README names the service, its dependencies and the environment variables `application.yml` requires.
