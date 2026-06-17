-- =====================================================================
-- The human-readable errand number now embeds year + MONTH (e.g.
-- EB-26060071) and the running counter restarts every month, so the
-- sequence gains a month column and the uniqueness key widens to include
-- it. Existing rows (year-only scheme) default to month 0 and are simply
-- left behind — the new month-scoped rows start fresh.
-- =====================================================================

alter table errand_number_sequence
    add column sequence_month int not null default 0 after sequence_year;

alter table errand_number_sequence
    drop index uq_errand_number_sequence;

alter table errand_number_sequence
    add constraint uq_errand_number_sequence unique (municipality_id, namespace, sequence_year, sequence_month);
