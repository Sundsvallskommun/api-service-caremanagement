package se.sundsvall.caremanagement.operaton.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;

import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

/**
 * A process message that did not reach the engine, kept for another attempt — the outbox behind finalize's
 * {@code PaymentDecisionReceived}. Written in the transaction that records the decision, so a saved decision can never
 * silently leave its process waiting before the decision gateway. {@code variables} holds the message's process
 * variables as JSON; they carry no personal data.
 */
@Entity
@Table(name = "process_message_retry",
	indexes = {
		@Index(name = "idx_process_message_retry_status_next_attempt", columnList = "status,next_attempt"),
		@Index(name = "idx_process_message_retry_errand_id", columnList = "errand_id")
	})
public class ProcessMessageRetryEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "errand_id", nullable = false, length = 36)
	private String errandId;

	@Column(name = "municipality_id", nullable = false, length = 8)
	private String municipalityId;

	@Column(name = "namespace", nullable = false, length = 32)
	private String namespace;

	@Column(name = "message_name", nullable = false, length = 64)
	private String messageName;

	@Column(name = "variables", length = 1024)
	private String variables;

	@Column(name = "status", nullable = false, length = 16)
	private String status;

	@Column(name = "attempts", nullable = false)
	private Integer attempts;

	@Column(name = "last_error", length = 1024)
	private String lastError;

	@Column(name = "next_attempt", nullable = false)
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime nextAttempt;

	@Column(name = "created", nullable = false)
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	public static ProcessMessageRetryEntity create() {
		return new ProcessMessageRetryEntity();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public ProcessMessageRetryEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public String getErrandId() {
		return errandId;
	}

	public void setErrandId(final String errandId) {
		this.errandId = errandId;
	}

	public ProcessMessageRetryEntity withErrandId(final String errandId) {
		this.errandId = errandId;
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public ProcessMessageRetryEntity withMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(final String namespace) {
		this.namespace = namespace;
	}

	public ProcessMessageRetryEntity withNamespace(final String namespace) {
		this.namespace = namespace;
		return this;
	}

	public String getMessageName() {
		return messageName;
	}

	public void setMessageName(final String messageName) {
		this.messageName = messageName;
	}

	public ProcessMessageRetryEntity withMessageName(final String messageName) {
		this.messageName = messageName;
		return this;
	}

	public String getVariables() {
		return variables;
	}

	public void setVariables(final String variables) {
		this.variables = variables;
	}

	public ProcessMessageRetryEntity withVariables(final String variables) {
		this.variables = variables;
		return this;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(final String status) {
		this.status = status;
	}

	public ProcessMessageRetryEntity withStatus(final String status) {
		this.status = status;
		return this;
	}

	public Integer getAttempts() {
		return attempts;
	}

	public void setAttempts(final Integer attempts) {
		this.attempts = attempts;
	}

	public ProcessMessageRetryEntity withAttempts(final Integer attempts) {
		this.attempts = attempts;
		return this;
	}

	public String getLastError() {
		return lastError;
	}

	public void setLastError(final String lastError) {
		this.lastError = lastError;
	}

	public ProcessMessageRetryEntity withLastError(final String lastError) {
		this.lastError = lastError;
		return this;
	}

	public OffsetDateTime getNextAttempt() {
		return nextAttempt;
	}

	public void setNextAttempt(final OffsetDateTime nextAttempt) {
		this.nextAttempt = nextAttempt;
	}

	public ProcessMessageRetryEntity withNextAttempt(final OffsetDateTime nextAttempt) {
		this.nextAttempt = nextAttempt;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public ProcessMessageRetryEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof final ProcessMessageRetryEntity other))
			return false;
		return Objects.equals(id, other.id) && Objects.equals(errandId, other.errandId) && Objects.equals(municipalityId, other.municipalityId) && Objects.equals(namespace, other.namespace) && Objects.equals(messageName, other.messageName) && Objects
			.equals(variables, other.variables) && Objects.equals(status, other.status) && Objects.equals(attempts, other.attempts) && Objects.equals(lastError, other.lastError) && Objects.equals(nextAttempt, other.nextAttempt) && Objects.equals(created,
				other.created);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, errandId, municipalityId, namespace, messageName, variables, status, attempts, lastError, nextAttempt, created);
	}

	@Override
	public String toString() {
		return "ProcessMessageRetryEntity{id='" + id + "', errandId='" + errandId + "', municipalityId='" + municipalityId + "', namespace='" + namespace + "', messageName='" + messageName + "', variables='" + variables + "', status='" + status
			+ "', attempts=" + attempts + ", lastError='" + lastError + "', nextAttempt=" + nextAttempt + ", created=" + created + "}";
	}
}
