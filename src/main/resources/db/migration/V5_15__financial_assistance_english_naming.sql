-- =====================================================================
-- Normalise the financial-assistance calculation schema to English naming.
-- The draft "normberäkning" tables/columns were created with Swedish
-- physical names (V5_5, V5_10, V5_11); the JPA entities now map to the
-- English names below, so rename the existing (already-deployed) schema
-- to match. RENAME preserves the data, and MariaDB updates the child
-- foreign keys that reference the renamed header table automatically.
-- =====================================================================

-- Header table: normberäkning draft -> calculation draft
rename table errand_financial_assistance_normberakning_draft
    to errand_financial_assistance_calculation_draft;

-- Incomes: handläggare amount -> caseworker amount (applicant + co-applicant)
alter table errand_fa_norm_income
    rename column applicant_handlaggare_amount to applicant_caseworker_amount,
    rename column coapplicant_handlaggare_amount to coapplicant_caseworker_amount;

-- Expenses: handläggare amount -> caseworker amount
alter table errand_fa_norm_expense
    rename column handlaggare_amount to caseworker_amount;

-- Persons: handläggare days -> caseworker days, jobbstimulans -> job stimulus
alter table errand_fa_norm_person
    rename column handlaggare_days to caseworker_days,
    rename column jobbstimulans_amount to job_stimulus_amount;
