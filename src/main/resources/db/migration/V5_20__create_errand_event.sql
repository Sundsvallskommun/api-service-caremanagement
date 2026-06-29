-- =====================================================================
-- Event log module — one row per errand-scoped HTTP request (who/what/
-- when). Populated by a HandlerInterceptor in afterCompletion, capturing
-- the dept44 Identifier (X-Sent-By actor) on the request thread.
--
-- Deliberately has NO foreign key to errand: an audit/access log must be
-- able to record a DELETE of an errand (the row is already gone when the
-- interceptor runs) and must survive errand removal.
-- =====================================================================

create table errand_event (
    id              varchar(255)    not null,
    errand_id       varchar(255)    not null,
    municipality_id varchar(16)     not null,
    namespace       varchar(64)     not null,
    action          varchar(16)     not null,
    target          varchar(255)    not null,
    description     varchar(512),
    http_method     varchar(8)      not null,
    request_path    varchar(1024)   not null,
    actor           varchar(255),
    actor_type      varchar(32),
    request_id      varchar(64),
    status_code     int             not null,
    created         datetime(6)     not null,
    primary key (id)
) engine = InnoDB;

create index idx_errand_event_errand_id         on errand_event (errand_id);
create index idx_errand_event_errand_id_created on errand_event (errand_id, created);
create index idx_errand_event_created           on errand_event (created);
