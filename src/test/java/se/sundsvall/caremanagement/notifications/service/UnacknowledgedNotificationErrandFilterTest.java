package se.sundsvall.caremanagement.notifications.service;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.core.integration.db.model.ErrandEntity;
import se.sundsvall.caremanagement.notifications.integration.db.model.NotificationEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnacknowledgedNotificationErrandFilterTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "FINANCIAL_ASSISTANCE";

	@Mock
	private Root<ErrandEntity> rootMock;

	@Mock
	private CriteriaQuery<?> queryMock;

	@Mock
	private CriteriaBuilder cbMock;

	@Mock
	private Subquery<String> subqueryMock;

	@Mock
	private Root<NotificationEntity> notificationRootMock;

	@Mock
	@SuppressWarnings("rawtypes")
	private Path pathMock;

	@Mock
	private Predicate predicateMock;

	@Mock
	private Predicate existsMock;

	private final UnacknowledgedNotificationErrandFilter filter = new UnacknowledgedNotificationErrandFilter();

	@SuppressWarnings("unchecked")
	private void stubCriteria() {
		when(queryMock.subquery(String.class)).thenReturn(subqueryMock);
		when(subqueryMock.from(NotificationEntity.class)).thenReturn(notificationRootMock);
		lenient().when(notificationRootMock.get(any(String.class))).thenReturn(pathMock);
		lenient().when(rootMock.get("id")).thenReturn(pathMock);
		lenient().when(cbMock.equal(any(), any())).thenReturn(predicateMock);
		lenient().when(cbMock.isFalse(any())).thenReturn(predicateMock);
		when(subqueryMock.select(any())).thenReturn(subqueryMock);
		when(subqueryMock.where(any(Predicate[].class))).thenReturn(subqueryMock);
		when(cbMock.exists(subqueryMock)).thenReturn(existsMock);
	}

	@Test
	void buildsExistsSubqueryScopedToOwner() {
		stubCriteria();

		final var spec = filter.hasUnacknowledgedNotifications(MUNICIPALITY_ID, NAMESPACE, "jane01doe");
		final var result = spec.toPredicate(rootMock, queryMock, cbMock);

		assertThat(result).isSameAs(existsMock);
		verify(cbMock).equal(pathMock, "jane01doe");
		verify(cbMock).isFalse(any());
		verify(cbMock).exists(subqueryMock);
	}

	@Test
	void buildsExistsSubqueryWithoutOwnerScope() {
		stubCriteria();

		final var spec = filter.hasUnacknowledgedNotifications(MUNICIPALITY_ID, NAMESPACE, null);
		final var result = spec.toPredicate(rootMock, queryMock, cbMock);

		assertThat(result).isSameAs(existsMock);
		verify(cbMock, never()).equal(pathMock, "jane01doe");
		verify(cbMock).exists(subqueryMock);
	}
}
