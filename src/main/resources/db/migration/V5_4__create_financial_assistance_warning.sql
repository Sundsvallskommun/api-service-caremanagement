-- =====================================================================
-- EB income warnings as first-class, acknowledgeable objects (instead of
-- free text inside the recommendation decision). The daily prepare step
-- reconciles them against the current SSBTEK picture — creating new ones,
-- auto-closing ones whose cause has resolved — and a handläggare can
-- acknowledge or close each one individually in Draken.
-- One row per (errand, warning); dedup is on (errand_id, type, source_key).
-- =====================================================================

create table errand_financial_assistance_warning (
    id            varchar(255) not null,
    errand_id     varchar(255) not null,
    type          varchar(64)  not null,
    source_key    varchar(255),
    message       longtext,
    status        varchar(32)  not null,
    auto_resolved bit(1)       not null default 0,
    created       datetime(6),
    updated       datetime(6),
    primary key (id)
);

create index idx_fa_warning_errand on errand_financial_assistance_warning (errand_id);
create index idx_fa_warning_dedup on errand_financial_assistance_warning (errand_id, type, source_key);
