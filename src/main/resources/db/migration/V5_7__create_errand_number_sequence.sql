-- =====================================================================
-- Per (municipality, namespace, year) running counter that backs the
-- human-readable errand number (e.g. EB_2026_0001). The prefix is the
-- namespace short code; the year resets the count; current_value is the
-- last handed-out number. One row per (municipality, namespace, year),
-- incremented under a pessimistic write lock when an errand is created.
-- =====================================================================

create table errand_number_sequence (
    id              bigint      not null auto_increment,
    municipality_id varchar(8)  not null,
    namespace       varchar(32) not null,
    sequence_year   int         not null,
    current_value   bigint      not null default 0,
    primary key (id)
);

alter table errand_number_sequence
    add constraint uq_errand_number_sequence unique (municipality_id, namespace, sequence_year);
