-- Rename the financial-assistance "bevakning" table + index to English (monitoring).
-- V5_14 created errand_financial_assistance_bevakning and is already deployed, so it
-- must stay untouched; this migration renames it to match the FaMonitoringEntity mapping.
rename table errand_financial_assistance_bevakning to errand_financial_assistance_monitoring;
alter table errand_financial_assistance_monitoring rename index idx_fa_bevakning_errand to idx_fa_monitoring_errand;
