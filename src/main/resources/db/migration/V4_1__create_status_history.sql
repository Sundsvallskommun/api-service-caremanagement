-- =====================================================================
-- Status history module — one row per status transition.
-- Populated by an @ApplicationModuleListener on ErrandStatusChanged.
-- =====================================================================

create table errand_status_history (
    id           varchar(255)    not null,
    errand_id    varchar(255)    not null,
    from_status  varchar(64),
    to_status    varchar(64)     not null,
    changed_by   varchar(64),
    changed_at   datetime(6)     not null,
    primary key (id)
) engine = InnoDB;

create index idx_status_history_errand_id  on errand_status_history (errand_id);
create index idx_status_history_changed_at on errand_status_history (changed_at);

alter table if exists errand_status_history
    add constraint fk_status_history_errand_id
    foreign key (errand_id)
    references errand (id)
    on delete cascade;
