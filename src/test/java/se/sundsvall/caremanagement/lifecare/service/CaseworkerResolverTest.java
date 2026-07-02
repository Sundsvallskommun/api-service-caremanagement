package se.sundsvall.caremanagement.lifecare.service;

import generated.se.sundsvall.lifecarefc.ApiPaginationCompositePersonBasedServiceDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedServiceDTO;
import generated.se.sundsvall.lifecarefc.User;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.lifecare.integration.LifecareFcIntegration;

import static java.time.Month.JUNE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseworkerResolverTest {

	private static final String PERSON_ID = "199001011234";
	private static final LocalDate DATE = LocalDate.of(2026, JUNE, 1);
	private static final int LOOKBACK_MONTHS = 36;
	private static final int USERS_LIMIT = 1000;

	@Mock
	private LifecareFcIntegration lifecareFcIntegrationMock;

	private CaseworkerResolver resolver;

	@BeforeEach
	void setUp() {
		resolver = new CaseworkerResolver(lifecareFcIntegrationMock, LOOKBACK_MONTHS, USERS_LIMIT);
	}

	@Test
	void resolvesMostRecentServiceCaseworkerToUserIds() {
		when(lifecareFcIntegrationMock.getServices(eq(PERSON_ID), any(), any())).thenReturn(
			new ApiPaginationCompositePersonBasedServiceDTO()
				.addResultItem(new PersonBasedServiceDTO().startDate("2024-01-01").caseworker("Old Caseworker"))
				.addResultItem(new PersonBasedServiceDTO().startDate("2026-02-01").caseworker("Anna Andersson")));
		when(lifecareFcIntegrationMock.getUsers(USERS_LIMIT, null, null, null)).thenReturn(List.of(
			new User().id("9001").fullName("Anna Andersson").networkUserId("anna01ker")));

		final var resolved = resolver.resolve(PERSON_ID, DATE);

		assertThat(resolved).hasValueSatisfying(caseworker -> {
			assertThat(caseworker.caseworkerId()).isEqualTo("9001");
			assertThat(caseworker.assignedUserId()).isEqualTo("anna01ker");
			assertThat(caseworker.fullName()).isEqualTo("Anna Andersson");
		});
	}

	@Test
	void queriesServicesOverTheLookbackWindow() {
		when(lifecareFcIntegrationMock.getServices(eq(PERSON_ID), eq("2023-06-01"), eq("2026-06-01")))
			.thenReturn(new ApiPaginationCompositePersonBasedServiceDTO());

		resolver.resolve(PERSON_ID, DATE);

		verify(lifecareFcIntegrationMock).getServices(PERSON_ID, "2023-06-01", "2026-06-01");
	}

	@Test
	void matchesFullNameCaseInsensitivelyAndTrimmed() {
		when(lifecareFcIntegrationMock.getServices(eq(PERSON_ID), any(), any())).thenReturn(
			new ApiPaginationCompositePersonBasedServiceDTO()
				.addResultItem(new PersonBasedServiceDTO().startDate("2026-02-01").caseworker("  anna andersson  ")));
		when(lifecareFcIntegrationMock.getUsers(USERS_LIMIT, null, null, null)).thenReturn(List.of(
			new User().id("9001").fullName("Anna Andersson").networkUserId("anna01ker")));

		assertThat(resolver.resolve(PERSON_ID, DATE)).map(ResolvedCaseworker::caseworkerId).contains("9001");
	}

	@Test
	void fallsBackToFcUserIdWhenNetworkUserIdBlank() {
		when(lifecareFcIntegrationMock.getServices(eq(PERSON_ID), any(), any())).thenReturn(
			new ApiPaginationCompositePersonBasedServiceDTO()
				.addResultItem(new PersonBasedServiceDTO().startDate("2026-02-01").caseworker("Anna Andersson")));
		when(lifecareFcIntegrationMock.getUsers(USERS_LIMIT, null, null, null)).thenReturn(List.of(
			new User().id("9001").fullName("Anna Andersson").networkUserId("   ")));

		assertThat(resolver.resolve(PERSON_ID, DATE)).map(ResolvedCaseworker::assignedUserId).contains("9001");
	}

	@Test
	void skipsDisabledUsersWhenMatching() {
		when(lifecareFcIntegrationMock.getServices(eq(PERSON_ID), any(), any())).thenReturn(
			new ApiPaginationCompositePersonBasedServiceDTO()
				.addResultItem(new PersonBasedServiceDTO().startDate("2026-02-01").caseworker("Anna Andersson")));
		when(lifecareFcIntegrationMock.getUsers(USERS_LIMIT, null, null, null)).thenReturn(List.of(
			new User().id("8000").fullName("Anna Andersson").networkUserId("old01ker").disabled(true),
			new User().id("9001").fullName("Anna Andersson").networkUserId("anna01ker")));

		assertThat(resolver.resolve(PERSON_ID, DATE)).map(ResolvedCaseworker::caseworkerId).contains("9001");
	}

	@Test
	void returnsEmptyWhenNoServices() {
		when(lifecareFcIntegrationMock.getServices(eq(PERSON_ID), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedServiceDTO());

		assertThat(resolver.resolve(PERSON_ID, DATE)).isEmpty();
		verify(lifecareFcIntegrationMock, never()).getUsers(any(), any(), any(), any());
	}

	@Test
	void returnsEmptyWhenServicesResponseIsNull() {
		when(lifecareFcIntegrationMock.getServices(eq(PERSON_ID), any(), any())).thenReturn(null);

		assertThat(resolver.resolve(PERSON_ID, DATE)).isEmpty();
	}

	@Test
	void returnsEmptyWhenNoServiceCarriesACaseworker() {
		when(lifecareFcIntegrationMock.getServices(eq(PERSON_ID), any(), any())).thenReturn(
			new ApiPaginationCompositePersonBasedServiceDTO()
				.addResultItem(new PersonBasedServiceDTO().startDate("2026-02-01")));

		assertThat(resolver.resolve(PERSON_ID, DATE)).isEmpty();
		verify(lifecareFcIntegrationMock, never()).getUsers(any(), any(), any(), any());
	}

	@Test
	void returnsEmptyWhenNoUserMatchesTheCaseworkerName() {
		when(lifecareFcIntegrationMock.getServices(eq(PERSON_ID), any(), any())).thenReturn(
			new ApiPaginationCompositePersonBasedServiceDTO()
				.addResultItem(new PersonBasedServiceDTO().startDate("2026-02-01").caseworker("Anna Andersson")));
		when(lifecareFcIntegrationMock.getUsers(USERS_LIMIT, null, null, null)).thenReturn(List.of(
			new User().id("9001").fullName("Bertil Bertilsson").networkUserId("bertil01")));

		assertThat(resolver.resolve(PERSON_ID, DATE)).isEmpty();
	}

	@Test
	void ignoresServicesWithMissingOrGarbledStartDateWhenPickingMostRecent() {
		when(lifecareFcIntegrationMock.getServices(eq(PERSON_ID), any(), any())).thenReturn(
			new ApiPaginationCompositePersonBasedServiceDTO()
				.addResultItem(new PersonBasedServiceDTO().caseworker("No Date Caseworker"))
				.addResultItem(new PersonBasedServiceDTO().startDate("not-a-date").caseworker("Garbled Date Caseworker"))
				.addResultItem(new PersonBasedServiceDTO().startDate("2026-02-01").caseworker("Anna Andersson")));
		when(lifecareFcIntegrationMock.getUsers(USERS_LIMIT, null, null, null)).thenReturn(List.of(
			new User().id("9001").fullName("Anna Andersson").networkUserId("anna01ker")));

		assertThat(resolver.resolve(PERSON_ID, DATE)).map(ResolvedCaseworker::fullName).contains("Anna Andersson");
	}

	@Test
	void returnsEmptyWithoutUserLookupWhenServiceResultListIsNull() {
		when(lifecareFcIntegrationMock.getServices(eq(PERSON_ID), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedServiceDTO().result(null));

		assertThat(resolver.resolve(PERSON_ID, DATE)).isEmpty();
	}
}
