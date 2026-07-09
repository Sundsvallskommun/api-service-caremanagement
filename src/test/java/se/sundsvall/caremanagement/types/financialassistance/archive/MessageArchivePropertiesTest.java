package se.sundsvall.caremanagement.types.financialassistance.archive;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import se.sundsvall.caremanagement.Application;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("junit")
class MessageArchivePropertiesTest {

	@Autowired
	private MessageArchiveProperties properties;

	@Test
	void testProperties() {
		assertThat(properties.municipalityId()).isEqualTo("2281");
		assertThat(properties.namespace()).isEqualTo("FINANCIAL_ASSISTANCE");
		assertThat(properties.daysAfterClose()).isEqualTo(30);
		assertThat(properties.lifecareDocumentType()).isEqualTo("MEDDELANDEHISTORIK");
		assertThat(properties.lifecareDocumentSenderType()).isEqualTo("MYNDIGHET");
		assertThat(properties.documentLabel()).isEqualTo("Meddelanden och bilagor från Draken");
		assertThat(properties.lifecareSenderName()).isEqualTo("Sundsvalls kommun");
	}
}
