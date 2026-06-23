-- =====================================================================
-- Document module — Dokument (formal case documents) attached to an
-- errand. Sibling of the journal entries: models the Lifecare document
-- shape (Typ, Rubrik, text, Datum/Tid, authorship) plus the skrivskydd
-- lifecycle (status WORKING -> LOCKED). LOCKED documents are immutable
-- upprättade handlingar.
-- =====================================================================

create table errand_document (
    id            varchar(255)    not null,
    errand_id     varchar(255)    not null,
    document_type varchar(255)    not null,
    heading       varchar(255)    not null,
    document_text longtext,
    document_date date            not null,
    document_time time,
    status        varchar(16)     not null,
    created_by    varchar(64),
    created       datetime(6)     not null,
    modified_by   varchar(64),
    modified      datetime(6),
    locked_by     varchar(64),
    locked        datetime(6),
    primary key (id)
) engine = InnoDB;

create index idx_document_errand_id     on errand_document (errand_id);
create index idx_document_document_date on errand_document (document_date);

alter table if exists errand_document
    add constraint fk_document_errand_id
    foreign key (errand_id)
    references errand (id)
    on delete cascade;
