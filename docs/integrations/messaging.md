# Messaging

api-service-messaging. Sends email.

## Used by

Nothing yet. `MessagingClient` has no callers in `src/main`. See [architecture.md](../architecture.md).

TODO: confirm whether email to applicants is still planned through this client, or whether the client can go.

## Calls

|              Call              |         Purpose         |
|--------------------------------|-------------------------|
| `POST /{municipalityId}/email` | Send a plain-text email |

## Authentication

OAuth2 client credentials, registration `messaging`.

## Code and configuration

- Client: `src/main/java/se/sundsvall/caremanagement/messaging/integration/`, models generated from
  `src/main/resources/integrations/messaging-api.yml`.
- Configuration: `integration.messaging` and the `messaging` registration in `application.yml`.

## Behaviour worth knowing

- The application still needs `INTEGRATION_MESSAGING_*` to start, although nothing sends mail.

