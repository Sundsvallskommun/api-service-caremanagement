package se.sundsvall.caremanagement.conversation.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReaderSideTest {

	@Test
	void caseworkerReadsInbound() {
		assertThat(ReaderSide.CASEWORKER.addressedDirection()).isEqualTo("INBOUND");
	}

	@Test
	void clientReadsOutbound() {
		assertThat(ReaderSide.CLIENT.addressedDirection()).isEqualTo("OUTBOUND");
	}
}
