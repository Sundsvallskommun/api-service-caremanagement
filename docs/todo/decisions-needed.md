# Decisions needed

## Keep or remove the messaging and templating clients

`messaging/integration/MessagingClient.java` and `templating/integration/TemplatingIntegration.java` have no callers in
`src/main`. The application still needs their `INTEGRATION_MESSAGING_*` and `INTEGRATION_TEMPLATING_*` environment
variables to start. See [messaging](../integrations/messaging.md) and [templating](../integrations/templating.md).

Done when they are either used by a flow or removed together with their configuration.

## Municipality id fixed in code for the integrator's ProfessionalWeb route

`lifecare/professionalweb/integrator/IntegratorProfessionalWebTransport.java` always calls the integrator with
municipality `2281`. That holds while there is one Lifecare, but it is the only municipality id in the integration
code that is not configuration.

Done when it is configuration, or an ADR records why it stays in code.

## Circuit breaker settings for the Lifecare clients

`application.yml` configures a circuit breaker only for `operaton`. The `lifecare-integrator`
and `lifecare-integrator-professionalweb` breakers run on whatever dept44 gives them by default. Not checked what that
is.

Done when the defaults are known and either accepted in [the integration files](../integrations/) or overridden in
`application.yml`.

## Unit tests build the schema with Hibernate, not Flyway

The `junit` profile sets Hibernate's schema action to `update` with Flyway off, so a migration missing for an entity
change is caught only by the integration tests (`it` validates against Flyway). See [data-model.md](../data-model.md).

Done when the unit tests run the migrations, or the team accepts that only the integration tests check them.
