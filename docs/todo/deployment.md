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
the open actualisation statuses, the users limit and the test decision maker. Which
environments are affected is not visible from this repo.

Done when every environment sets the four variables before this version is deployed there, and the unused ones are
removed from its configuration.

## lifecare-integrator's ProfessionalWeb resources must be deployed first

careM calls lifecare-integrator's ProfessionalWeb resources under `/{municipalityId}/professional-web/` and no longer
its `POST /{municipalityId}/professional-web/exchange`. An integrator without those resources answers 404 on every
ProfessionalWeb call. The print templates of the decision, calculation and document PDFs are now set in the
integrator: careM no longer reads `INTEGRATION_LIFECARE_PROFESSIONALWEB_DECISION_PRINT_TEMPLATE_ID`,
`_CALCULATION_PRINT_TEMPLATE_ID` or `_DOCUMENT_PRINT_TEMPLATE_ID`.

Done when every environment runs an integrator with the resources and its print templates before this version is
deployed there, and the three unused variables are removed from careM's configuration.
