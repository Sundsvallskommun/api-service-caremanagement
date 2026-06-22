-- =====================================================================
-- EB monitorings — date-bound watch/reminder objects on an errand. Unlike
-- the income warnings (errand_financial_assistance_warning) these carry no
-- acknowledge lifecycle: a caseworker simply creates, edits and removes
-- them, and each has a start date (when the watch becomes relevant) and an
-- optional end date. Modelled after Lifecare IFO's "Monitorings".
-- One row per (errand, monitoring).
-- =====================================================================

create table errand_financial_assistance_monitoring (
    id          varchar(255) not null,
    errand_id   varchar(255) not null,
    title       varchar(255) not null,
    description longtext,
    start_date  date         not null,
    end_date    date,
    created_by  varchar(255),
    created     datetime(6),
    updated     datetime(6),
    primary key (id)
);

create index idx_fa_monitoring_errand on errand_financial_assistance_monitoring (errand_id);
