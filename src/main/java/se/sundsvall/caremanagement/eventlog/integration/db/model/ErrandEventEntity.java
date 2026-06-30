package se.sundsvall.caremanagement.eventlog.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;

import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

@Entity
@Table(name = "errand_event",
	indexes = {
		@Index(name = "idx_errand_event_errand_id", columnList = "errand_id"),
		@Index(name = "idx_errand_event_errand_id_created", columnList = "errand_id, created"),
		@Index(name = "idx_errand_event_created", columnList = "created")
	})
public class ErrandEventEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "errand_id", nullable = false, length = 255)
	private String errandId;

	@Column(name = "municipality_id", nullable = false, length = 16)
	private String municipalityId;

	@Column(name = "namespace", nullable = false, length = 64)
	private String namespace;

	@Column(name = "source", nullable = false, length = 16)
	private String source;

	@Column(name = "action", nullable = false, length = 16)
	private String action;

	@Column(name = "target", nullable = false, length = 255)
	private String target;

	@Column(name = "description", length = 512)
	private String description;

	@Column(name = "http_method", length = 8)
	private String httpMethod;

	@Column(name = "request_path", length = 1024)
	private String requestPath;

	@Column(name = "actor", length = 255)
	private String actor;

	@Column(name = "actor_type", length = 32)
	private String actorType;

	@Column(name = "request_id", length = 64)
	private String requestId;

	@Column(name = "status_code")
	private Integer statusCode;

	@Column(name = "created", nullable = false)
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	public static ErrandEventEntity create() {
		return new ErrandEventEntity();
	}

	public String getId() {
		return id;
	}

	public String getErrandId() {
		return errandId;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public String getNamespace() {
		return namespace;
	}

	public String getSource() {
		return source;
	}

	public String getAction() {
		return action;
	}

	public String getTarget() {
		return target;
	}

	public String getDescription() {
		return description;
	}

	public String getHttpMethod() {
		return httpMethod;
	}

	public String getRequestPath() {
		return requestPath;
	}

	public String getActor() {
		return actor;
	}

	public String getActorType() {
		return actorType;
	}

	public String getRequestId() {
		return requestId;
	}

	public Integer getStatusCode() {
		return statusCode;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public ErrandEventEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public ErrandEventEntity withErrandId(final String errandId) {
		this.errandId = errandId;
		return this;
	}

	public ErrandEventEntity withMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public ErrandEventEntity withNamespace(final String namespace) {
		this.namespace = namespace;
		return this;
	}

	public ErrandEventEntity withSource(final String source) {
		this.source = source;
		return this;
	}

	public ErrandEventEntity withAction(final String action) {
		this.action = action;
		return this;
	}

	public ErrandEventEntity withTarget(final String target) {
		this.target = target;
		return this;
	}

	public ErrandEventEntity withDescription(final String description) {
		this.description = description;
		return this;
	}

	public ErrandEventEntity withHttpMethod(final String httpMethod) {
		this.httpMethod = httpMethod;
		return this;
	}

	public ErrandEventEntity withRequestPath(final String requestPath) {
		this.requestPath = requestPath;
		return this;
	}

	public ErrandEventEntity withActor(final String actor) {
		this.actor = actor;
		return this;
	}

	public ErrandEventEntity withActorType(final String actorType) {
		this.actorType = actorType;
		return this;
	}

	public ErrandEventEntity withRequestId(final String requestId) {
		this.requestId = requestId;
		return this;
	}

	public ErrandEventEntity withStatusCode(final Integer statusCode) {
		this.statusCode = statusCode;
		return this;
	}

	public ErrandEventEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}
}
