package se.sundsvall.caremanagement.cocaseworkers.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

@Schema(description = "A co-caseworker (medhandläggare) on an errand. Both the errand's ordinary caseworker and every "
	+ "co-caseworker receive the errand's notifications as one shared, logical message")
public class CoCaseworker {

	@Schema(description = "Unique identifier", examples = "cb20c51f-fcf3-42c0-b613-de563634a8ec")
	private String id;

	@Schema(description = "Errand id this co-caseworker is added to")
	private String errandId;

	@Schema(description = "User id of the co-caseworker", examples = "jane01doe")
	private String userId;

	@Schema(description = "When the co-caseworker was added")
	@DateTimeFormat(iso = DATE_TIME)
	private OffsetDateTime created;

	public static CoCaseworker create() {
		return new CoCaseworker();
	}

	public String getId() {
		return id;
	}

	public String getErrandId() {
		return errandId;
	}

	public String getUserId() {
		return userId;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public void setErrandId(final String errandId) {
		this.errandId = errandId;
	}

	public void setUserId(final String userId) {
		this.userId = userId;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public CoCaseworker withId(final String id) {
		this.id = id;
		return this;
	}

	public CoCaseworker withErrandId(final String errandId) {
		this.errandId = errandId;
		return this;
	}

	public CoCaseworker withUserId(final String userId) {
		this.userId = userId;
		return this;
	}

	public CoCaseworker withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final CoCaseworker that = (CoCaseworker) o;
		return Objects.equals(id, that.id) && Objects.equals(errandId, that.errandId)
			&& Objects.equals(userId, that.userId) && Objects.equals(created, that.created);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, errandId, userId, created);
	}

	@Override
	public String toString() {
		return "CoCaseworker{id='" + id + "', errandId='" + errandId + "', userId='" + userId + "', created=" + created + "}";
	}
}
