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
import se.sundsvall.caremanagement.cocaseworkers.integration.db.model.CoCaseworkerEntity;
import se.sundsvall.caremanagement.core.integration.db.model.ErrandEntity;
import se.sundsvall.caremanagement.notifications.integration.db.model.NotificationEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationErrandFilterTest {

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
	private Subquery<String> coCaseworkerSubqueryMock;

	@Mock
	private Root<CoCaseworkerEntity> coCaseworkerRootMock;

	@Mock
	@SuppressWarnings("rawtypes")
	private Path pathMock;

	@Mock
	private Predicate predicateMock;

	@Mock
	private Predicate existsMock;

	@Mock
	private Predicate coCaseworkerExistsMock;

	@Mock
	private Predicate orMock;

	private final NotificationErrandFilter filter = new NotificationErrandFilter();

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

		lenient().when(subqueryMock.subquery(String.class)).thenReturn(coCaseworkerSubqueryMock);
		lenient().when(coCaseworkerSubqueryMock.from(CoCaseworkerEntity.class)).thenReturn(coCaseworkerRootMock);
		lenient().when(coCaseworkerRootMock.get(any(String.class))).thenReturn(pathMock);
		lenient().when(coCaseworkerSubqueryMock.select(any())).thenReturn(coCaseworkerSubqueryMock);
		lenient().when(coCaseworkerSubqueryMock.where(any(Predicate[].class))).thenReturn(coCaseworkerSubqueryMock);
		lenient().when(cbMock.exists(coCaseworkerSubqueryMock)).thenReturn(coCaseworkerExistsMock);
		lenient().when(cbMock.or(any(), any())).thenReturn(orMock);
	}

	@Test
	void buildsExistsSubqueryScopedToOwner() {
		stubCriteria();

		final var spec = filter.hasUnacknowledgedNotifications(MUNICIPALITY_ID, NAMESPACE, "jane01doe");
		final var result = spec.toPredicate(rootMock, queryMock, cbMock);

		assertThat(result).isSameAs(existsMock);
		verify(cbMock, times(2)).equal(pathMock, "jane01doe");
		verify(cbMock).isFalse(any());
		verify(notificationRootMock).get("acknowledged");
		verify(cbMock).exists(subqueryMock);
		// widens the owner match with a nested co-caseworker EXISTS, correlated on the same errand
		verify(coCaseworkerSubqueryMock).from(CoCaseworkerEntity.class);
		verify(cbMock).exists(coCaseworkerSubqueryMock);
		verify(cbMock).or(any(), same(coCaseworkerExistsMock));
	}

	@Test
	void buildsExistsSubqueryWithoutOwnerScope() {
		stubCriteria();

		final var spec = filter.hasUnacknowledgedNotifications(MUNICIPALITY_ID, NAMESPACE, null);
		final var result = spec.toPredicate(rootMock, queryMock, cbMock);

		assertThat(result).isSameAs(existsMock);
		verify(cbMock, never()).equal(pathMock, "jane01doe");
		verify(cbMock).exists(subqueryMock);
		verify(cbMock, never()).exists(coCaseworkerSubqueryMock);
	}

	@Test
	void buildsUnhandledExistsSubqueryScopedToOwner() {
		stubCriteria();

		final var spec = filter.hasUnhandledNotifications(MUNICIPALITY_ID, NAMESPACE, "jane01doe");
		final var result = spec.toPredicate(rootMock, queryMock, cbMock);

		assertThat(result).isSameAs(existsMock);
		verify(cbMock, times(2)).equal(pathMock, "jane01doe");
		verify(notificationRootMock).get("handled");
		verify(notificationRootMock, never()).get("acknowledged");
		verify(cbMock).exists(subqueryMock);
		verify(cbMock).exists(coCaseworkerSubqueryMock);
	}

	@Test
	void buildsUnhandledExistsSubqueryWithoutOwnerScope() {
		stubCriteria();

		final var spec = filter.hasUnhandledNotifications(MUNICIPALITY_ID, NAMESPACE, null);
		final var result = spec.toPredicate(rootMock, queryMock, cbMock);

		assertThat(result).isSameAs(existsMock);
		verify(cbMock, never()).equal(pathMock, "jane01doe");
		verify(notificationRootMock).get("handled");
		verify(cbMock).exists(subqueryMock);
		verify(cbMock, never()).exists(coCaseworkerSubqueryMock);
	}
}
