# Changelog

Every change to the service, newest date first. One short line per change.

## 2026-11-23

- Keep household members without a partyId in the Lifecare roster, previous family and previous household, and mark the household incomplete.
- Reach Lifecare FamilyCare and ProfessionalWeb only through lifecare-integrator, and drop the direct route with its settings.
- Add docs/ with architecture, data model and integration notes, AGENTS.md and CHANGELOG.md.

## 2026-10-13

- Run the expired-notification cleanup in a transaction.

## 2026-10-09

- Bump dept44-service-parent to 8.0.12.

## 2026-10-05

- Write-protect a Lifecare decision only when its message has text, with the check moved out of `LifecareDecisionBodies.fill`.
- Remove the normberäkning that loses the race to be linked to the errand.
- Show a financial assistance errand only the applicant's ekonomiskt bistånd records from Lifecare.

## 2026-10-02

- Stop wrapping the whole actualisation step in one transaction.
- Read the protected-identity flags the way Citizen v3 sends them.

## 2026-10-01

- Turn `spring.jpa.open-in-view` off.
- Drop a queued process message that no process can take, and keep Operaton's 404 out of the circuit breaker.

## 2026-09-30

- Print a Lifecare decision whose template asks parameter questions.
- Register a Lifecare decision for a household with a co-applicant.
- Add job stimulus periods for the co-applicant.
- Bump dept44-service-parent to 8.0.11.
- Save a normberäkning for a household with a co-applicant.
- Carry the previous calculation's days into a new one.
- Make the Lifecare decision from the errand's normberäkning.
- Remove a job stimulus period in Lifecare.
- Return only the SSBTEK payment fields the caseworker's view needs.
- End the process when an errand is withdrawn.
- Let a decision be finalized when the errand is back in `SUPPLEMENT_REQUESTED`.

## 2026-09-29

- Never create a second Lifecare actualisation on a retry.
- Start the process again for an errand whose first start was lost.
- Switch off the recently-closed guard until it reads Lifecare.
- Hold an errand for manual review when a party has protected identity or the flag cannot be checked.
- Propose the previous decision reason only when the new decision goes the same way.
- Document that the SSBTEK route forwards the agency answers with their identity fields.
- Remove the Lifecare history routes that were not scoped to an errand.
- Log the reads of a blank journal note, document and reminder, and test that every Lifecare read fails closed.
- Group journal notes and documents by kind, and show document types 14 and 15.
- Squash the Flyway migrations into a single `V1_0`.
- Remove the local Lifecare copies and the BFF report endpoints.

## 2026-09-28

- Read the payment linked to the errand as registered, and propose its month.
- Link either the service or the investigation on a Lifecare actualisation, never both.
- Print documents with the "Brev Flerval adress SV" template by default.
- Print written documents and forms from Lifecare as PDF with a print template.

## 2026-09-26

- Save the draft's notes, dates and gross amounts to Lifecare.

## 2026-09-25

- Compare incomes with the previous normberäkning.
- Keep calculation notes within Lifecare's 80 characters.
- Carry the incomes declared in the application into the normberäkning draft.
- Close draft warnings that the saved Lifecare calculation settles.
- Base the decision proposal on the saved Lifecare calculation.
- Warn on SSBTEK changes, not on caseworker edits.
- Serve an applicant's Lifecare document as a PDF file.
- Stop duplicate expense warnings.
- Correct the decision proposal and the Lifecare calculation proposal.
- Stop expecting applicant-reported incomes from SSBTEK.
- Create the Lifecare normberäkning proposal on the first prepare run.
- Keep the ProfessionalWeb exchange out of payload logging.
- Reach ProfessionalWeb through lifecare-integrator.
- Never link a new application to the person's existing Lifecare cases.
- Actualise a new application with the new-application actualisation type.
- Edit the normberäkning in Lifecare through ProfessionalWeb.
- Transfer household children's SSBTEK incomes.
- Finalize from the Lifecare decision and receipt it in the process.
- Read, save and print the errand's decision in Lifecare, and add the section status.
- Add payments, payees and job stimulus through ProfessionalWeb.
- Fix how Spring picks the `ProfessionalWebHttp` constructor.
- Add reminders, journal notes and documents through ProfessionalWeb.
- Add the errand context and the access log for ProfessionalWeb calls.
- Add the ProfessionalWeb transport and the integration-account session.
- Keep the Lifecare normberäkning in step with SSBTEK.
- Read the SSBTEK basis of a household child.

## 2026-09-24

- Add per-user settings, with a flag to open SSBTEK in a new window.
- Drop `phraseText` from the decision proposal.
- Stop dropping SSBTEK incomes whose category Lifecare names differently.
- Pass the partyId straight through to the Lifecare integrator route.
- Create the normberäkning proposal in Lifecare from prepare.
- Rename rpa-context to household-identifiers, and remove the old alias.
- Remove the section approvals.
- Drop the retired payment table.
- Decide the proposed outcome on the saved Lifecare calculation.
- Purge the frozen normberäkning draft at finalize.
- Remove the unused `jobStimulusAmount` from the normberäkning draft.
- Read granted payments from Lifecare instead of local payment drafts.
- Write notification descriptions in Swedish.
- Add contract integration tests for `lifecareCalculationId`.
- Keep a failed Lifecare insats lookup from failing the errand read.
- Make the FLAG-05 recovery-claim lookback configurable.
- Raise `LIFECARE_READ_FAILED` instead of closing warnings when a Lifecare read fails.
- Compare NORM-04 common household costs against the application.
- Refuse a granting finalize without `lifecareCalculationId`.
- Freeze the housing-cost warning with the draft.
- Let Draken own the Lifecare normberäkning.
- Bind the test server to 127.0.0.1 so a random port cannot be shadowed.
- Scope the manual SSBTEK read to an errand.
- Show the applicant's recovery claims (återkrav) on the decision tab.
- Copy the norm and the family from the previous normberäkning.
- Run finalize's process message retry end to end in a test.
- Retire the RPA integration.
- Count midsommarafton as a day of allowance in the day check.
- Keep the co-applicant's reason through finalize.

## 2026-09-23

- Retry a decision message that did not reach the process.
- Say when a granted decision's payments are overdue.
- Let the caseworker set a Lifecare decision id on the errand.
- Verify the errand's own payments before it goes to `PAID`.
- Remove the payment proposal endpoint.
- Create the Lifecare calculation under the errand's ekonomiskt bistånd insats only.
- Count julafton and nyårsafton as days of allowance.
- Match an advance on a benefit (förskott på förmån) on the decision type only.
- Resolve a caseworker-entered income's Lifecare type from its name.
- Propose the payee with the address Lifecare last paid to.
- Carry a payee picked straight from Lifecare through finalize.
- Count Saturdays as red days in the daily allowance day check.
- Show only ekonomiskt bistånd decisions from Lifecare.
- Let a caseworker add and remove co-caseworkers on an errand.
- Gate the daily allowance day check and count the days that are not red.
- Archive the application PDF together with the merged attachments.
- Report whether a decision made it into Lifecare.
- Log the reads and writes careM makes in Lifecare directly.
- Carry the Lifecare insats id on the errand.
- Let the RPA robot be turned off one action at a time.
- Give the integrator route 90 seconds to answer.
- Send the document type ids that Lifecare's catalogue uses.
- Keep the integrator's explanation instead of "Unknown error".
- Read the combined application PDF by the type it is stored under.
- Archive the application on the actualisation step.

## 2026-09-22

- Calculate against the norm the application asked for, within the days the period allows.
- Stop seeding personal numbers into the process.
- Link only the service and investigation that the actualisation type accepts.
- Add an amount per household member on the draft's person row.
- Mark a notification as handled separately from having read it.
- Declare the property that selects the FamilyCare route, so its environment variable is named.
- Resolve the previous household to personnummer on both FamilyCare routes.
- Place an errand on a caseworker only through an open insats.
- Send personnummer, not partyIds, in FamilyCare `CalculationPersons`.
- Follow up the access log per user, and log search hits.
- Warn when an income from the comparison period is transferred late.
- Set the application date (Ansökningsdatum) from the submission date.
- Move the Lifecare writes, the two proposals and the remaining reads to the integrator route.
- Read calculations through the lifecare-integrator route.
- Put FamilyCare behind an interface so the integrator route can be used.
- Add the SSBTEK basis endpoint to the financial assistance API.
- Let the `REGISTER_PAYMENT` robot report back what Lifecare did.
- Link a payment to its payee row, so the robot gets the Lifecare payee id.
- Declare `X-Sent-By` as a global apiKey scheme in the OpenAPI document.
- Document that Mina sidor does not read the type catalogue.
- Flush the payment rows before finalize touches anything external.
- Widen the payment status column so finalize can write a row.
- Retire the internet and föräldrapenning period checks, and load the list of payment methods.

## 2026-09-21

- Warn on finalize when a payee is not in Lifecare yet.
- Keep the REGISTER_PAYMENT contract working for the robot.
- Let the caseworker pick a betalningsmottagare, with a manual list.
- Drop the AVVISNING decision outcome and label DELAVSLAG "Delvis bifall".
- Timestamp the warning raised when SSBTEK could not be read.
- Propose an orsak for the co-applicant too.
- Make the warnings and the calculation rows readable.
- Keep the payee's bank details out of the RPA queue.
- Pick the actualisation codes verksamheten named instead of the first one Lifecare offers.
- Warn when an income SSBTEK reported last month is gone.
- Leave the calculation unchanged when SSBTEK could not be read.
- Read Lifecare times in the Swedish time zone, and stop writing unchanged values to the local copies.
- Restore the applied V1_0 baseline migration and move the payment tables to V1_4.
- Use Lifecare's own orsak catalogue in the decision proposal.
- Add a payment resource for creating, reading, updating and removing utbetalningar.
- Add the "Besluta och utbetala" finalize endpoint.
- Add decision and payment proposal endpoints.

## 2026-09-18

- Run the SSBTEK period checks in the daily preparation.
- Use FamilyCare's real type names instead of guessed ones.

## 2026-09-17

- Read SSBTEK day counts as decimals instead of whole numbers.
- Raise the återansökan rule warnings from the new DMN tables.
- Read the income amounts and norm of the previous normberäkning.
- Add a check for whether an errand has an application attachment.

## 2026-09-11

- Keep the sick-leave period from the application.
- Transfer only the comparison-period incomes the previous month missed.
- Carry the SSBTEK period and day count through to the calculation.
- Flag expenses the rules never assessed.
- Warn when an income is counted twice in the norm.

## 2026-09-10

- Fix the norm types not being copied into the calculation draft.

## 2026-09-02

- Resolve the partyId in rpa-context from the errand's stakeholders first.

## 2026-09-01

- Bump dept44-service-parent to 8.0.10.

## 2026-08-28

- Put the errand number instead of personnummer on RPA queue items, and add an rpa-context endpoint.

## 2026-08-26

- Add an endpoint where the RPA robot delivers its Lifecare data for an errand.

## 2026-08-25

- Complete the routing rules of the common entry point.

## 2026-08-20

- Authenticate to the FamilyCare user directory with its own key.
- Send FamilyCare dates as RFC 3339.

## 2026-08-18

- Bump dept44-service-parent to 8.0.9.

## 2026-08-17

- Remove the draft calculation entry points that nothing called.
- Carry money as BigDecimal and spell out FamilyCare instead of FC.
- Bind the financial assistance create request part as a typed object.

## 2026-07-13

- Bump dept44-service-parent to 8.0.8.

## 2026-07-10

- Release a frozen errand into the process of its own application type.
- Check that the errand is in the caller's scope when a calculation is prepared or committed.
- Enforce unique Lifecare ids on monitoring rows and unique namespace short codes, folded into the V1_0 baseline.
- Give the financial assistance data update patch semantics, and validate the UUID path variables.
- Make the eligibility return-all-types override an environment setting, and drop the unused X-Sent-By header on message create.
- Align the test suite with the dept44 conventions.

## 2026-07-09

- Rename Swedish-English identifiers to English and stop mixing languages in strings.
- Split the financial assistance service into one service per resource, with the calculation service on its own.
- Validate the errandId of a calculation request in the model instead of the service.
- Replace the nested items list of a form snapshot with named groups.
- Name the form snapshot service methods after what they do.
- Add tests for the status history listener.

## 2026-07-08

- Fill in the 404 responses in the API, add OpenAPI examples, and drop redundant indexes.
- Limit String fields in the API to the length of their database columns.
- Leave large text columns out of entity equals, hashCode and toString.

## 2026-07-07

- Remove an errand's event log when the errand is deleted instead of on a timer.
- Record the message-history marker before the Lifecare upload in the archive job.
- Fail the build on single-letter setter parameters.
- Take the section approver from X-Sent-By.
- Route reads of an errand from other modules through a query interface in core.
- Fix a missing-role NullPointerException, the audit actor and locking after review.
- Split the financial assistance resource into eight resources, one per domain, with their own services and tests.
- Declare the 400 and 500 responses on every resource.

## 2026-07-06

- Declare full response sets on the decision, stakeholder and document resources.
- Pass the decision key as a query parameter when evaluating a DMN table in Operaton.

## 2026-07-03

- Validate a form snapshot before checking the database.

## 2026-07-02

- Remove the unused Eneo integration.
- Size the journal entry text to the 1 MB API limit.
- Merge a journal entry's date and time into one entryDateTime.
- Rename DecisionCreated.typeSlug to decisionType, and set default timeouts for the financial assistance integrations.
- Keep the interceptor that requires an identifier on every request, and drop @ValidIdentifier.
- Name create handlers create<Entity> and their events <Entity>Created.
- Handle notifications with @ApplicationModuleListener.

## 2026-07-01

- Record decisions, messages, stakeholder changes, journal entries, notes and documents in the event log.
- Index attachments on errand and origin.
- Fix equals and hashCode on the attachment data entity.
- Route errand existence checks through a shared ErrandAccessGuard.
- Make financial assistance errands deletable with ON DELETE CASCADE, and drop the delete listeners the cascade replaces.
- Collapse SenderRole and ReaderSide into Direction in the conversation module.
- Reject a missing X-Sent-By by default.
- Scope errand events by tenant fully.

## 2026-06-30

- Align the entities' column lengths with the migrated column sizes.
- Reject a typed-create-only errand type on the generic `POST /errands`.
- Close a cross-tenant IDOR in journal entries and documents, and restore the type column length.
- Remove the duplicated errand-existence check and fix a `@Transactional` self-invocation.
- Squash the 49 migrations into one baseline and right-size the columns.
- Sanitize user-derived values in log statements against log injection.
- Bring the full drakel module set onto the PR: financial assistance and all type modules.
- Add field-by-field tests for the core mappers.
- Decouple permits and referrals from the errand foreign key.

## 2026-06-29

- Validate path variables on the create endpoints.
- Remove dead code and stale docs across modules, and tidy `toString` output.
- Give mark-as-read a clearer error when only some messages are found.
- Rename the attachment field `origin` to `documentType`.
- Scope the conversation endpoints to their errand and fix a null pointer on status.
- Sanitize uploaded filenames before logging them.
- Compare byte-array records by content in `equals`, `hashCode` and `toString`.
- Migrate the errand platform to Spring Modulith.
- Harden access control, concurrency and personal data handling across the sprint's changes.

## 2026-06-26

- Accept a single value where the financial assistance application expects a list, and report the real parse error.
- Retry the financial assistance errand-created listener when MariaDB 11's snapshot isolation refuses it.
- Accept the financial assistance application's `request` part without a JSON content type.
- Add Swedish display names to notification types and subtypes.
- Write the new-message notification text in Swedish.
- Write the event log descriptions in Swedish.
- Serve Lifecare calculations, decisions and documents to the frontend.
- Write the financial assistance warnings in Swedish and add enum display names.

## 2026-06-25

- Add the expense rule tree per cost type from verksamhetens skälighetstabell.
- Start the new-application process automatically for new financial assistance errands.
- Keep a caseworker-edited applied amount on system expense rows across the daily refresh.
- Make the applicant name on the errand list sortable and searchable.
- Start the supplementary-application process for supplementary errands.
- Seed the financial assistance status display names into the editable `/metadata` lookup store.
- Expose statuses with display names and lifecycle order on `/errand-types`.
- Record the chosen actualisation id on the errand when archiving to it.
- List a person's Lifecare actualisations and archive an uploaded document to one.
- Add the recently-closed guard, which recommends a renewal and freezes a re-application for manual review.

## 2026-06-24

- Create a normberäkning straight from a new application.
- Assign the default caseworker through the `Decision_defaultAssignee` DMN.
- Give each calculation draft row a stable position within its section.
- Make the applied amount on norm expenses patchable.
- Notify on an inbound message even when the errand is unassigned.
- Stamp the errand with the time of the last daily run.
- Add the form snapshot: an immutable, re-renderable copy of the citizen's application form.

## 2026-06-23

- Set the caseworker on the actualisation and the errand from the applicant's Lifecare service.
- Add per-errand badge counts for notes, events, monitorings and active warnings.
- Archive the message history only when the applicant has written, leave out the caseworker's attachments, and add a period line.
- Add the unread message count and mark-as-read receipts.
- Require `X-Sent-By` on the municipality-scoped API, matching only numeric municipality paths.
- Add a UiPath Orchestrator trigger module with write and fetch hooks for financial assistance.
- Give the event log readable descriptions and filters for source and reads.
- Add source and Lifecare id to monitorings, for the RPA mirror of Lifecare's bevakningar.
- Record domain events in the event log, with a source field.
- Add a per-errand who/what/when activity log covering reads and writes.
- Store the case data PDF (ärendeuppgifter) with a financial assistance errand on create, named `{errandNumber}.pdf`, one per errand.
- Add the case data origin to attachments and make the origin optional on upload.
- Split the financial assistance Swagger tag into seven groups.
- Shorten the external label of occupational pension income to "Tjänstepension/försäkringar".

## 2026-06-22

- Add the caseworker-only Lifecare types to the financial assistance metadata catalogue.
- Make `TypeOption.group` a stable code (HOUSING, WORK_AND_STUDIES, HEALTH, OTHER).
- Add the financial assistance type metadata endpoint, with Mina sidor and Lifecare labels, groups and a citizen-reportable flag.
- Accept the applied amount when a norm expense is created.
- Restore the citizen income and cost contract, which a type-metadata change had broken for Mina sidor.
- Add journal entries with create, read, update and delete, and a write-protection lock on errands, with the journal type catalogue in the metadata lookup store.
- Rename bevakning to monitoring in the new migration and restore an already-applied one.
- Rename the financial assistance and Lifecare API to English.
- Add monitorings (bevakningar), date-bound watch objects with create, read, update and delete.
- Flag renewal changes in household size and housing through a DMN.
- Show warnings from the expense rules for reasonableness review and caps.

## 2026-06-18

- Add caseworker approvals per section: calculation, payment and decision.
- Add the decision tab API with enriched decisions, decision alternatives and the recommendation on the financial assistance view.
- Allow a financial assistance warning to be reopened.
- Align the normberäkning with Lifecare FamilyCare's sections, in a draft of persons, incomes and expenses.
- Tag the combined client PDF with the origin CONVERSATION.
- Filter the attachment list by origin and sender role.

## 2026-06-17

- Number errands from the namespace short code as `EB-YYMM####`, with a monthly sequence.
- Tag attachments and combine the client conversation into one PDF.
- Make the draft normberäkning editable, with overwrite protection.
- Make the financial assistance income warnings objects a caseworker acknowledges.
- Split the normberäkning into preparing it, without Lifecare, and committing it after the decision.
- Report how complete the normberäkning is compared with the previous month.
- Seed the personnummer and the SSBTEK window into the financial assistance process.
- Move the SSBTEK rules out of caremanagement.
- Store the applicants as stakeholders when an errand is created.
- Build the normberäkning from the incomes Operaton classified.
- Fit images onto A4 pages when combining attachments.
- Add the payment status endpoint, which reads Lifecare's payments.
- Store the Spring Modulith `event_publication` id as `binary(16)` on MariaDB 11.
- Start the `rakel-ekonomiskt-bistand` process when a renewal comes in.
- Add the create-actualisation endpoint for Lifecare case intake.

## 2026-06-16

- Let a message reply to another message (`inReplyToId`).
- Raise the multipart limits to 50 MB per file and no limit per request.
- Keep protected identity out of the eligibility response.
- Allow notes to be edited, and add the protected-identity flag to eligibility.
- Include legacy `.doc` files in the combined PDF.
- Support file attachments on messages.
- Accept attachments when an errand is created and store them as one combined PDF.
- Expose a form schema per type slug through `/errand-types`.
- Record the SSBTEK normberäkning warnings as a RECOMMENDATION decision.
- Add a notification channel and contact details per person.
- Identify parties by partyId across the financial assistance API, including the renewal prefill.
- Assemble the SSBTEK normberäkning and post it to Lifecare FamilyCare.
- Return only the children from the renewal prefill.
- Silence dept44 payload logging to stop personnummer and keys leaking in URIs, then restore full request logging.

## 2026-06-15

- Extract incomes from Försäkringskassan's data.
- Make the eligibility window service configuration instead of a request field.
- Align eligibility with the agreed common-entry-point flow.
- Add the SSBTEK income pipeline: fetch, extract, prepare and map to the FamilyCare normberäkning.
- Add the application-eligibility endpoint (gemensam ingång).
- Warn when an SSBTEK income differs by more than 12%.
- Add the SSBTEK period and transfer rule.
- Add the financial-aid integration, the SSBTEK proxy.
- Add person-based FamilyCare read endpoints.
- Add the Lifecare FamilyCare write-back integration.

## 2026-06-11

- Target the api-service-citizen v3 contract.
- Add an Eneo (LLM) integration client for decision support.
- Add the api-service-citizen lookup client.
- Add the api-service-templating client.
- Add the api-service-messaging client.
- Add the referral module.
- Add the permit module.
- Add a demo-only scheduler that resets the database nightly.
- Add the read-only errand statistics endpoint.
- Add the conversation module, a message thread between caseworker and applicant.
- Give each errand a readable errand number when it is stored.

## 2026-06-10

- Add a multi-stage Dockerfile for source-based builds.
- Stop using the system clock in tests (HYDRAN-2114).

## 2026-05-26

- Bump dept44-service-parent to 8.0.7.

## 2026-05-13

- Try out new concepts, work in progress (HYDRAN-2214).

## 2026-05-11

- Add notifications (HYDRAN-2111).

## 2026-04-28

- Add the decisions model and adjust the Operaton integration (HYDRAN-2037).

## 2026-04-23

- Start processes from the service layer (HYDRAN-1999).

## 2026-04-22

- Add integration tests (HYDRAN-1991).

## 2026-04-21

- Add the Operaton integration (HYDRAN-1941).

## 2026-04-16

- Add the service layer (HYDRAN-1940).
- Add the API model and resources (HYDRAN-1936).

## 2026-04-15

- Add the entities (HYDRAN-1939).

## 2026-04-14

- Rename the project to caremanagement (HYDRAN-1925).
