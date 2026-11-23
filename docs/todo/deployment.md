# Deployment

## Every environment needs the lifecare-integrator variables

careM reaches Lifecare only through api-service-lifecare-integrator, and the application does not start without
`INTEGRATION_LIFECARE_INTEGRATOR_URL`, `INTEGRATION_LIFECARE_INTEGRATOR_CLIENT_ID`,
`INTEGRATION_LIFECARE_INTEGRATOR_CLIENT_SECRET` and `INTEGRATION_LIFECARE_INTEGRATOR_TOKEN_URI`. Environments that
used to call Lifecare directly set the direct route's variables instead, and the new code ignores them:

- `INTEGRATION_LIFECARE_FAMILYCARE_` followed by `URL`, `DOMAIN`, `KEY`, `USER_KEY`, `PROVIDER`, `CONNECT_TIMEOUT` or
  `READ_TIMEOUT`.
- `INTEGRATION_LIFECARE_PROFESSIONALWEB_` followed by `URL`, `DOMAIN`, `FEDERATION_PROFILE`, `USERNAME`, `PASSWORD`,
  `SESSION_TTL`, `PROVIDER`, `CONNECT_TIMEOUT` or `READ_TIMEOUT`.

The other variables under those two prefixes still bind, such as the lookback months, the caseworker lookback months,
the open actualisation statuses, the users limit, the test decision maker and the print template ids. Which
environments are affected is not visible from this repo.

Done when every environment sets the four variables before this version is deployed there, and the unused ones are
removed from its configuration.
