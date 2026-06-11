-- =====================================================================
-- Notes module — free-form notes attached to an errand.
-- =====================================================================

create table errand_note (
    id          varchar(255)    not null,
    errand_id   varchar(255)    not null,
    body        longtext        not null,
    author      varchar(64),
    created     datetime(6)     not null,
    primary key (id)
) engine = InnoDB;

create index idx_note_errand_id on errand_note (errand_id);
create index idx_note_created   on errand_note (created);

alter table if exists errand_note
    add constraint fk_note_errand_id
    foreign key (errand_id)
    references errand (id)
    on delete cascade;
