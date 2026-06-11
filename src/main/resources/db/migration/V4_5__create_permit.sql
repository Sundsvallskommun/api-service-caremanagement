-- =====================================================================
-- Permit module — a structured, time-bound, revocable permit issued on an
-- errand. Type-agnostic; cleaned up via an ErrandDeleted listener.
-- =====================================================================

create table permit (
    id          varchar(255)    not null,
    errand_id   varchar(255)    not null,
    permit_type varchar(64),
    valid_from  date,
    valid_until date,
    conditions  varchar(4096),
    status      varchar(32),
    created     datetime(6),
    modified    datetime(6),
    primary key (id)
) engine = InnoDB;

create index idx_permit_errand_id on permit (errand_id);

alter table if exists permit
    add constraint fk_permit_errand_id
    foreign key (errand_id)
    references errand (id);
