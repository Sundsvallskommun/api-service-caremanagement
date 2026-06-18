-- =====================================================================
-- Align the normberäkning draft with the Lifecare FC PostCalculationBodyRequest
-- (and the Beräkning GUI it mirrors): four section arrays + household.
--
--  FAMILJ                  → calculation_persons (+ norm, + household)
--  INKOMSTER               → one row per type with a sökande (S) + medsökande (M) side
--  UTGIFTER                → expenses, bucket = EXPENSE          (CalculationExpenses)
--  LEVNADSKOSTNADER I ÖVR. → expenses, bucket = SPECIAL_EXPENSE  (CalculationSpecialExpenses)
--  GEMENSAMMA KOSTNADER    → has_custom_household_size + household_size
-- =====================================================================

-- Inkomster: one row per FC income type, each side keeping its process vs handläggare value.
alter table errand_fa_norm_income
    drop column recipient,
    drop column process_amount,
    drop column process_amount_date,
    drop column handlaggare_amount,
    drop column handlaggare_amount_date,
    add column applicant_process_amount       decimal(12, 2),
    add column applicant_handlaggare_amount   decimal(12, 2),
    add column applicant_amount_date          datetime(6),
    add column coapplicant_process_amount     decimal(12, 2),
    add column coapplicant_handlaggare_amount decimal(12, 2),
    add column coapplicant_amount_date        datetime(6);

-- Utgifter: bucket splits regular expenses from levnadskostnader i övrigt / särskilda kostnader.
-- Set by the Decision_utgiftRegelverk DMN per cost type; handläggare-added rows carry it explicitly.
alter table errand_fa_norm_expense
    add column bucket varchar(20) not null default 'EXPENSE';

-- Personer: full Familj — omfattas, avvikelseperiod (ingår från/till), normintervall, jobbstimulans.
-- role also carries UMGANGESBARN (umgängesbarn = part-time/visitation child via process_days).
alter table errand_fa_norm_person
    add column included             bit(1) not null default 1,
    add column deviation_from_date  date,
    add column deviation_to_date    date,
    add column norm_interval        varchar(64),
    add column jobbstimulans_amount decimal(12, 2);

-- Header: FC calculation dates + custom household size (gemensamma kostnader).
alter table errand_financial_assistance_normberakning_draft
    add column calculation_from_date     date,
    add column calculation_to_date       date,
    add column calculation_date          date,
    add column has_custom_household_size bit(1),
    add column household_size            int;
