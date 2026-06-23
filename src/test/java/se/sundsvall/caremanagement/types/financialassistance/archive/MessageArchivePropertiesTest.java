package se.sundsvall.caremanagement.types.financialassistance.archive;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

class MessageArchivePropertiesTest {

	@Test
	void bindsAllFields() {
		final var source = new MapConfigurationPropertySource(Map.of(
			"archive.message.municipality-id", "1984",
			"archive.message.namespace", "MY_NAMESPACE",
			"archive.message.days-after-close", "7",
			"archive.message.lifecare-document-type", "DOC",
			"archive.message.lifecare-document-sender-type", "SENDER",
			"archive.message.document-label", "Label",
			"archive.message.lifecare-sender-name", "Sender"));

		final var properties = new Binder(source).bind("archive.message", MessageArchiveProperties.class).get();

		assertThat(properties.municipalityId()).isEqualTo("1984");
		assertThat(properties.namespace()).isEqualTo("MY_NAMESPACE");
		assertThat(properties.daysAfterClose()).isEqualTo(7);
		assertThat(properties.lifecareDocumentType()).isEqualTo("DOC");
		assertThat(properties.lifecareDocumentSenderType()).isEqualTo("SENDER");
		assertThat(properties.documentLabel()).isEqualTo("Label");
		assertThat(properties.lifecareSenderName()).isEqualTo("Sender");
	}

	@Test
	void appliesDefaults() {
		final var properties = new Binder(new MapConfigurationPropertySource(Map.of()))
			.bindOrCreate("archive.message", MessageArchiveProperties.class);

		assertThat(properties.municipalityId()).isEqualTo("2281");
		assertThat(properties.namespace()).isEqualTo("FINANCIAL_ASSISTANCE");
		assertThat(properties.daysAfterClose()).isEqualTo(30);
		assertThat(properties.lifecareDocumentType()).isEqualTo("MEDDELANDEHISTORIK");
		assertThat(properties.lifecareDocumentSenderType()).isEqualTo("MYNDIGHET");
		assertThat(properties.documentLabel()).isEqualTo("Meddelanden och bilagor från Draken");
		assertThat(properties.lifecareSenderName()).isEqualTo("Sundsvalls kommun");
	}
}
