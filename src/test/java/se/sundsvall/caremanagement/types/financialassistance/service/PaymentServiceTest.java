package se.sundsvall.caremanagement.types.financialassistance.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Payment;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FaPaymentRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaPaymentEntity;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = "errand-1";

	@Mock
	private ErrandService errandServiceMock;

	@Mock
	private FaPaymentRepository repositoryMock;

	@InjectMocks
	private PaymentService service;

	private static FaPaymentEntity entity(final String id, final OffsetDateTime created) {
		return FaPaymentEntity.create().withId(id).withErrandId(ERRAND_ID).withApplicationMonth("2026-08").withCreated(created);
	}

	@Test
	void listReturnsMappedSortedByCreated() {
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of(
			entity("b2", OffsetDateTime.parse("2026-06-02T00:00:00Z")),
			entity("b1", OffsetDateTime.parse("2026-06-01T00:00:00Z"))));

		final var result = service.list(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(result).extracting("id").containsExactly("b1", "b2"); // created asc
		verify(errandServiceMock).readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void scopeCheckPropagatesWhenErrandMissing() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenThrow(Problem.valueOf(NOT_FOUND, "Errand not found"));

		assertThatThrownBy(() -> service.list(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND)
			.hasMessage("Not Found: Errand not found");

		verify(repositoryMock, never()).findByErrandId(any());
	}

	@Test
	void lifecareIdsOnOtherErrandsAsksTheRepository() {
		when(repositoryMock.findLifecareIdsOnOtherErrands(List.of("101", "102"), ERRAND_ID)).thenReturn(List.of("102"));

		assertThat(service.lifecareIdsOnOtherErrands(List.of("101", "102"), ERRAND_ID)).containsExactly("102");
		verifyNoInteractions(errandServiceMock);
	}

	@Test
	void lifecareIdsOnOtherErrandsWithoutIdsDoesNotQuery() {
		assertThat(service.lifecareIdsOnOtherErrands(List.of(), ERRAND_ID)).isEmpty();
		verifyNoInteractions(repositoryMock);
	}

	@Test
	void listCarriesWhatPaymentStatusReads() {
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of(entity("b1", OffsetDateTime.parse("2026-06-01T00:00:00Z"))
			.withSource("CASEWORKER").withStatus("REGISTERED").withLifecareId("101").withPaymentDate(LocalDate.of(2026, 8, 25))
			.withAmount(new BigDecimal("4500.00"))));

		assertThat(service.list(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).singleElement()
			.returns("CASEWORKER", Payment::getSource)
			.returns("REGISTERED", Payment::getStatus)
			.returns("101", Payment::getLifecareId)
			.returns(LocalDate.of(2026, 8, 25), Payment::getPaymentDate)
			.returns("2026-08", Payment::getApplicationMonth)
			.returns(OffsetDateTime.parse("2026-06-01T00:00:00Z"), Payment::getCreated);
	}
}
