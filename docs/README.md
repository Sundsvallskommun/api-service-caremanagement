# Documentation

Getting started and configuration are in the [root README](../README.md). The API contract is
[openapi.yaml](../src/integration-test/resources/api/openapi.yaml). Changes are listed in the
[changelog](../CHANGELOG.md).

|                                       Document                                       |                                           Covers                                           |
|--------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------|
| [architecture.md](architecture.md)                                                   | Modules, flows, scheduled jobs, cross-cutting rules, and which endpoint reads which source |
| [data-model.md](data-model.md)                                                       | Tables, the entities that map them, and how the schema is changed                          |
| [integrations/operaton.md](integrations/operaton.md)                                 | The BPMN engine that runs each errand's process and the rule tables                        |
| [integrations/lifecare-familycare.md](integrations/lifecare-familycare.md)           | Lifecare's public API: case history, actualisations, calculations, payments                |
| [integrations/lifecare-professionalweb.md](integrations/lifecare-professionalweb.md) | Lifecare's internal api2: normberäkning, decisions, payments, records, reminders           |
| [integrations/lifecare-integrator.md](integrations/lifecare-integrator.md)           | careM's only way to Lifecare, for both FamilyCare and ProfessionalWeb                      |
| [integrations/citizen.md](integrations/citizen.md)                                   | partyId ↔ personnummer, and protected identity                                             |
| [integrations/financial-aid.md](integrations/financial-aid.md)                       | SSBTEK payments from other agencies                                                        |
| [integrations/messaging.md](integrations/messaging.md)                               | Email, client without callers                                                              |
| [integrations/templating.md](integrations/templating.md)                             | PDF rendering, client without callers                                                      |
| [todo/cleanup.md](todo/cleanup.md)                                                   | Comments, references and code to correct or remove                                         |
| [todo/decisions-needed.md](todo/decisions-needed.md)                                 | Behaviour that needs a decision before it changes                                          |
| [todo/deployment.md](todo/deployment.md)                                             | Configuration each environment needs before a deploy                                       |

Ownership, environments and contacts: Confluence. TODO: link to the service page.
