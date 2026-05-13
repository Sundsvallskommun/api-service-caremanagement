-- =====================================================================
-- Spring Modulith event publication outbox.
-- Owned by spring-modulith-starter-jpa via JpaEventPublication entity
-- (table name "event_publication", id BINARY(16) per @Column(length = 16) UUID).
-- =====================================================================

create table event_publication (
    id                     binary(16)      not null,
    listener_id            varchar(255)    not null,
    event_type             varchar(255)    not null,
    serialized_event       longtext        not null,
    publication_date       datetime(6)     not null,
    completion_date        datetime(6),
    status                 varchar(255),
    completion_attempts    integer,
    last_resubmission_date datetime(6),
    primary key (id)
) engine = InnoDB;

create index idx_event_publication_completion_date on event_publication (completion_date);
