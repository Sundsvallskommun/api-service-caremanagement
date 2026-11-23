# Deployment

## Every environment needs the lifecare-integrator variables

careM reaches Lifecare only through api-service-lifecare-integrator, and the application does not start without
`INTEGRATION_LIFECARE_INTEGRATOR_URL`, `INTEGRATION_LIFECARE_INTEGRATOR_CLIENT_ID`,
`INTEGRATION_LIFECARE_INTEGRATOR_CLIENT_SECRET` and `INTEGRATION_LIFECARE_INTEGRATOR_TOKEN_URI`. Environments that
used to call Lifecare directly set the old `INTEGRATION_LIFECARE_FAMILYCARE_*` and
`INTEGRATION_LIFECARE_PROFESSIONALWEB_*` variables instead. The new code ignores those, apart from
`INTEGRATION_LIFECARE_PROFESSIONALWEB_TEST_DECISION_MAKER`. Which environments are affected is not visible from this
repo.

Done when every environment sets the four variables before this version is deployed there, and the unused ones are
removed from its configuration.
