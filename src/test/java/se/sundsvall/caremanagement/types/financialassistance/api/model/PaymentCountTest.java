package se.sundsvall.caremanagement.types.financialassistance.api.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentCountTest {

	@Test
	void testAccessor() {
		assertThat(new PaymentCount(2).count()).isEqualTo(2);
	}
}
