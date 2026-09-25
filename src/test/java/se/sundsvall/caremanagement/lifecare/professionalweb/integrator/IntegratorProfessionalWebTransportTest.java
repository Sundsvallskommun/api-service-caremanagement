package se.sundsvall.caremanagement.lifecare.professionalweb.integrator;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IntegratorProfessionalWebTransportTest {

	@Mock
	private ProfessionalWebIntegratorClient client;

	@InjectMocks
	private IntegratorProfessionalWebTransport transport;

	@Captor
	private ArgumentCaptor<ProfessionalWebExchangeRequest> requestCaptor;

	@Test
	void packsTheCallAndUnpacksTheAnswer() {
		final var answer = Base64.getEncoder().encodeToString("{\"exceptionMessage\":\"Nej\"}".getBytes(StandardCharsets.UTF_8));
		when(client.exchange(eq("2281"), any())).thenReturn(new ProfessionalWebExchangeResponse(461, "application/json", answer));
		final var params = new LinkedHashMap<String, String>();
		params.put("businessType", "8");

		final var response = transport.exchange("POST", "api2/Decision/Create", params, "{\"a\":1}".getBytes(StandardCharsets.UTF_8));

		assertThat(response.status()).isEqualTo(461);
		assertThat(response.contentType()).isEqualTo("application/json");
		assertThat(response.bodyAsString()).contains("Nej");
		verify(client).exchange(eq("2281"), requestCaptor.capture());
		assertThat(requestCaptor.getValue().method()).isEqualTo("POST");
		assertThat(requestCaptor.getValue().path()).isEqualTo("api2/Decision/Create");
		assertThat(requestCaptor.getValue().params()).containsExactly(Map.entry("businessType", "8"));
		assertThat(requestCaptor.getValue().body().get("a").asInt()).isEqualTo(1);
	}

	@Test
	void emptyBodyAndNoContentType() {
		when(client.exchange(eq("2281"), any())).thenReturn(new ProfessionalWebExchangeResponse(200, null, null));

		final var response = transport.exchange("GET", "api2/x", Map.of(), new byte[0]);

		assertThat(response.isSuccess()).isTrue();
		assertThat(response.body()).isEmpty();
		assertThat(response.contentType()).isEmpty();
		verify(client).exchange(eq("2281"), requestCaptor.capture());
		assertThat(requestCaptor.getValue().body()).isNull();
	}

	@Test
	void noAnswer() {
		assertThatThrownBy(() -> transport.exchange("GET", "api2/x", Map.of(), null)).hasMessageContaining("without Lifecare's answer");
	}
}
