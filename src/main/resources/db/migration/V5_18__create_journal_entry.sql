-- =====================================================================
-- Journal module — journalanteckningar (case-journal entries) attached
-- to an errand. Models the Lifecare journal shape (Typ, Rubrik, text,
-- Datum/Tid, authorship) plus the skrivskydd lifecycle (status WORKING ->
-- LOCKED). LOCKED entries are immutable upprättade handlingar.
-- =====================================================================

create table errand_journal_entry (
    id          varchar(255)    not null,
    errand_id   varchar(255)    not null,
    entry_type  varchar(255)    not null,
    heading     varchar(255)    not null,
    entry_text  longtext,
    entry_date  date            not null,
    entry_time  time,
    status      varchar(16)     not null,
    created_by  varchar(64),
    created     datetime(6)     not null,
    modified_by varchar(64),
    modified    datetime(6),
    locked_by   varchar(64),
    locked      datetime(6),
    primary key (id)
) engine = InnoDB;

create index idx_journal_entry_errand_id  on errand_journal_entry (errand_id);
create index idx_journal_entry_entry_date on errand_journal_entry (entry_date);

alter table if exists errand_journal_entry
    add constraint fk_journal_entry_errand_id
    foreign key (errand_id)
    references errand (id)
    on delete cascade;
