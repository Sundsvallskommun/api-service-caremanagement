# Citizen

api-service-citizen v3. Translates the partyId the API uses into the personnummer that Lifecare and SSBTEK
need, and tells whether a person has a protected identity.

## Used by

Other modules call it through `CitizenService`. See [architecture.md](../architecture.md).

| Method | Callers |
|---|---|
| `getPersonalNumber` (partyId → personnummer) | `FinancialAssistanceCalculationService`, `FinancialAssistanceSsbtekService`, `HouseholdIdentifiersService`, `LifecareErrandService`, `NormberakningDraftReader` |
| `hasProtectedIdentity` | `EligibilityService`, `ProtectedIdentityGate` |

## Calls

| Call | Purpose |
|---|---|
| `GET /{municipalityId}/{personId}/personnumber` | partyId → personnummer |
| `GET /{municipalityId}/{personId}?ShowClassified=false` | The person's protection flags |

## Authentication

OAuth2 client credentials, registration `citizen`.

## Code and configuration

- Client: `src/main/java/se/sundsvall/caremanagement/citizen/integration/`, models generated from
  `src/main/resources/integrations/citizen-api.yml`.
- Service: `src/main/java/se/sundsvall/caremanagement/citizen/service/CitizenService.java`.
- Configuration: `integration.citizen` and the `citizen` registration in `application.yml`. The URL must end in the
  v3 base path, `/api/v3/citizen`.

## Behaviour worth knowing

- Feign logging is off for this client, since both lookups carry a personnummer in the URL or the body.
- A 204 from Citizen is an empty `Optional` from the lookups.
- `hasProtectedIdentity` is true when `classified` or `protectedNR` holds anything other than `N`, and also when Citizen
  returns no person. Citizen leaves a classified person out of an answer without `ShowClassified`, so an empty answer
  is never taken as clear.
- Lookups are not cached. Each call reaches Citizen.
