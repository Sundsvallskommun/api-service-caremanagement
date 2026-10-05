package se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.problem.Problem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;

@ExtendWith(MockitoExtension.class)
class SurplusCalculationRemoverTest {

	@Mock
	private LifecareCalculationClient client;

	@InjectMocks
	private SurplusCalculationRemover remover;

	@Test
	void removesTheBeräkningFromLifecare() {
		assertThat(remover.remove(53)).isTrue();

		verify(client).delete(53);
	}

	@Test
	void leavesItWhenLifecareRefuses() {
		doThrow(Problem.valueOf(BAD_GATEWAY, "Lifecare refused")).when(client).delete(53);

		assertThat(remover.remove(53)).isFalse();
	}

	@Test
	void leavesItWhenLifecareCannotBeReached() {
		doThrow(new IllegalStateException("no session")).when(client).delete(53);

		assertThat(remover.remove(53)).isFalse();
	}
}
