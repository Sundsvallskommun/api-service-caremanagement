package se.sundsvall.caremanagement.conversation.spi;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DirectionTest {

	@Test
	void hasExactlyInboundAndOutbound() {
		assertThat(Direction.values()).containsExactly(Direction.INBOUND, Direction.OUTBOUND);
		assertThat(Direction.valueOf("INBOUND")).isEqualTo(Direction.INBOUND);
	}
}
