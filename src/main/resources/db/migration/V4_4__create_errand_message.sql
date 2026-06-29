-- =====================================================================
-- Conversation module — messages between caseworker and applicant on an
-- errand. Content lives only in this in-app thread.
-- =====================================================================

create table errand_message (
    id          varchar(255)    not null,
    errand_id   varchar(255)    not null,
    direction   varchar(16)     not null,
    body        longtext        not null,
    author      varchar(64),
    created     datetime(6)     not null,
    primary key (id)
) engine = InnoDB;

create index idx_message_errand_id on errand_message (errand_id);
create index idx_message_created   on errand_message (created);

alter table if exists errand_message
    add constraint fk_message_errand_id
    foreign key (errand_id)
    references errand (id)
    on delete cascade;
