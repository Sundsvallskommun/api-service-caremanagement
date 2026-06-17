-- =====================================================================
-- Editable draft normberäkning. During the daily prepare loop the EB
-- process stores the computed FC income rows here (without writing to
-- Lifecare); a handläggare can read + edit them in Draken before deciding.
-- While the draft is untouched the daily refresh overwrites the rows; once
-- edited (edited = 1) the rows are preserved and newly-arrived SSBTEK income
-- is surfaced as a NEW_INCOME warning instead. On a beslut the (possibly
-- edited) draft is what gets posted to Lifecare.
-- One row per errand; the rows themselves are a JSON array.
-- =====================================================================

create table errand_financial_assistance_normberakning_draft (
    errand_id         varchar(255) not null,
    application_month varchar(7),
    edited            bit(1)       not null default 0,
    rows_json         longtext,
    created           datetime(6),
    updated           datetime(6),
    primary key (errand_id)
);
