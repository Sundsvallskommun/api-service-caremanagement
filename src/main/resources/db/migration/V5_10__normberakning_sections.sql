-- =====================================================================
-- Normberäkning gains its full three-section shape: personer, inkomster
-- and utgifter. The draft is no longer a single JSON blob with one
-- whole-draft "edited" flag — each row now carries its own provenance.
--
-- Every section row keeps the value the PROCESS decided (system-owned,
-- written only by the daily prepare) separate from the value a
-- HANDLÄGGARE decided (human-owned, written only from Draken), can be
-- soft-deleted (deleted = 1, survives the daily refresh — never
-- resurrected), and records who created it (origin). The effective value
-- posted to Lifecare on a beslut is the handläggare value when set, else
-- the process value.
--
-- The header keeps one row per errand and now also carries the selected
-- norm; rows_json + edited are gone (ownership is per-row now).
-- =====================================================================

alter table errand_financial_assistance_normberakning_draft
    add column norm_id   int,
    add column norm_type varchar(32),
    drop column rows_json,
    drop column edited;

-- Inkomster — one row per (FC income type, recipient). Subtracted from the norm.
create table errand_fa_norm_income (
    id                      varchar(255) not null,
    errand_id               varchar(255) not null,
    origin                  varchar(16)  not null,
    type_id                 int,
    type_name               varchar(255),
    recipient               varchar(16)  not null,
    process_amount          decimal(12, 2),
    process_amount_date     datetime(6),
    handlaggare_amount      decimal(12, 2),
    handlaggare_amount_date datetime(6),
    deleted                 bit(1)       not null default 0,
    note                    longtext,
    created                 datetime(6),
    updated                 datetime(6),
    primary key (id),
    constraint fk_fa_norm_income_draft foreign key (errand_id)
        references errand_financial_assistance_normberakning_draft (errand_id) on delete cascade
);

create index idx_fa_norm_income_errand on errand_fa_norm_income (errand_id);

-- Utgifter — one row per applied cost. Added to the norm. applied = what the
-- citizen asked for; process = the regelverk cap; handlaggare = the override.
create table errand_fa_norm_expense (
    id                 varchar(255) not null,
    errand_id          varchar(255) not null,
    origin             varchar(16)  not null,
    cost_type          varchar(64)  not null,
    other_sub_type     varchar(32),
    specification      longtext,
    applied_amount     decimal(12, 2),
    process_amount     decimal(12, 2),
    handlaggare_amount decimal(12, 2),
    deleted            bit(1)       not null default 0,
    note               longtext,
    created            datetime(6),
    updated            datetime(6),
    primary key (id),
    constraint fk_fa_norm_expense_draft foreign key (errand_id)
        references errand_financial_assistance_normberakning_draft (errand_id) on delete cascade
);

create index idx_fa_norm_expense_errand on errand_fa_norm_expense (errand_id);

-- Personer — one row per household member. Drives the norm base + the
-- comparison against the previous normberäkning in Lifecare.
create table errand_fa_norm_person (
    id               varchar(255) not null,
    errand_id        varchar(255) not null,
    origin           varchar(16)  not null,
    party_id         varchar(255),
    role             varchar(16)  not null,
    name             varchar(255),
    process_days     int,
    handlaggare_days int,
    deleted          bit(1)       not null default 0,
    note             longtext,
    created          datetime(6),
    updated          datetime(6),
    primary key (id),
    constraint fk_fa_norm_person_draft foreign key (errand_id)
        references errand_financial_assistance_normberakning_draft (errand_id) on delete cascade
);

create index idx_fa_norm_person_errand on errand_fa_norm_person (errand_id);
