-- =====================================================================
-- Form-snapshot module — an immutable, self-describing record of the
-- citizen-facing application form exactly as it was rendered and
-- answered in Mina sidor: every question, help/info/notice text, option
-- label and answer. Authored by the frontend at submission time and
-- stored verbatim (payload) with a SHA-256 content hash, so it can be
-- read back and displayed "as it was" with no access to the frontend.
-- Write-once: one snapshot per errand (uq_form_snapshot_errand), no
-- update path. Lives and dies with the errand (on delete cascade).
-- =====================================================================

create table errand_form_snapshot (
    id                      varchar(255) not null,
    errand_id               varchar(255) not null,
    municipality_id         varchar(32)  not null,
    namespace               varchar(128) not null,
    type_slug               varchar(64)  not null,
    schema_version          varchar(32)  not null,
    form_definition_version varchar(64),
    locale                  varchar(16),
    content_hash            char(64)     not null,
    payload                 longtext     not null,
    captured_at             datetime(6),
    created                 datetime(6)  not null,
    primary key (id),
    constraint uq_form_snapshot_errand unique (errand_id)
) engine = InnoDB;

create index idx_form_snapshot_errand_id on errand_form_snapshot (errand_id);

alter table if exists errand_form_snapshot
    add constraint fk_form_snapshot_errand_id
    foreign key (errand_id)
    references errand (id)
    on delete cascade;
