-- =====================================================================
-- EB section approvals — a handläggare's verification that one section of
-- the Draken EB view (CALCULATION = normberäkning, PAYMENT = utbetalning,
-- DECISION = beslut) has been reviewed and approved. One row per
-- (errand, section); the section is acknowledged (approved=1) or its
-- approval is withdrawn (approved=0). approved_by / approved_at carry who
-- approved it and when, and are cleared when the approval is withdrawn.
-- =====================================================================

create table errand_financial_assistance_section_approval (
    id          varchar(255) not null,
    errand_id   varchar(255) not null,
    section     varchar(32)  not null,
    approved    bit(1)       not null default 0,
    approved_by varchar(255),
    approved_at datetime(6),
    created     datetime(6),
    updated     datetime(6),
    primary key (id)
);

create index idx_fa_section_approval_errand on errand_financial_assistance_section_approval (errand_id);
create unique index uq_fa_section_approval on errand_financial_assistance_section_approval (errand_id, section);
