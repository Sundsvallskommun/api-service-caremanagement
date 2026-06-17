package se.sundsvall.caremanagement.namespaceconfig.service;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.namespaceconfig.integration.db.NamespaceConfigRepository;
import se.sundsvall.caremanagement.namespaceconfig.integration.db.model.NamespaceConfigEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NamespaceConfigShortCodeResolverTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "FINANCIAL_ASSISTANCE";

	@Mock
	private NamespaceConfigRepository repositoryMock;

	@InjectMocks
	private NamespaceConfigShortCodeResolver resolver;

	@Test
	void returnsShortCodeWhenConfigured() {
		when(repositoryMock.findByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(NamespaceConfigEntity.create().withShortCode("EB")));

		assertThat(resolver.resolvePrefix(MUNICIPALITY_ID, NAMESPACE)).contains("EB");
	}

	@Test
	void returnsEmptyWhenNoConfig() {
		when(repositoryMock.findByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.empty());

		assertThat(resolver.resolvePrefix(MUNICIPALITY_ID, NAMESPACE)).isEmpty();
	}

	@Test
	void returnsEmptyWhenShortCodeBlank() {
		when(repositoryMock.findByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(NamespaceConfigEntity.create().withShortCode("  ")));

		assertThat(resolver.resolvePrefix(MUNICIPALITY_ID, NAMESPACE)).isEmpty();
	}

	@Test
	void returnsEmptyWhenShortCodeNull() {
		when(repositoryMock.findByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(NamespaceConfigEntity.create()));

		assertThat(resolver.resolvePrefix(MUNICIPALITY_ID, NAMESPACE)).isEmpty();
	}
}
