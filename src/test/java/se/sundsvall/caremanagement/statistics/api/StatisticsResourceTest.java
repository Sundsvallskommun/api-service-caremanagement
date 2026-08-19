package se.sundsvall.caremanagement.statistics.api;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.statistics.api.model.AssigneeCount;
import se.sundsvall.caremanagement.statistics.api.model.StatisticsResponse;
import se.sundsvall.caremanagement.statistics.api.model.StatusCount;
import se.sundsvall.caremanagement.statistics.service.StatisticsService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class StatisticsResourceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String PATH = "/{municipalityId}/{namespace}/statistics";

	@MockitoBean
	private StatisticsService serviceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void getStatistics() {
		final var expected = new StatisticsResponse(3L, List.of(new StatusCount("NEW", 2L)), List.of(new AssigneeCount("user1", 1L)), 1L);
		when(serviceMock.compute(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(), any(), any())).thenReturn(expected);

		final var response = webTestClient.get()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isOk()
			.expectBody(StatisticsResponse.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.total()).isEqualTo(3L);
		assertThat(response.unassigned()).isEqualTo(1L);
		verify(serviceMock).compute(MUNICIPALITY_ID, NAMESPACE, null, null, null);
	}

	@Test
	void getStatisticsFilteredByType() {
		when(serviceMock.compute(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq("TYPE-1"), isNull(), isNull()))
			.thenReturn(new StatisticsResponse(0L, List.of(), List.of(), 0L));

		webTestClient.get()
			.uri(uri -> uri.path(PATH).queryParam("typeSlug", "TYPE-1").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isOk();

		verify(serviceMock).compute(MUNICIPALITY_ID, NAMESPACE, "TYPE-1", null, null);
	}
}
