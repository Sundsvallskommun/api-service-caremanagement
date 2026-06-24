-- =====================================================================
-- Stamp the last time the EB process ran its daily loop for the errand.
--
--   last_daily_run_at — set every time the process calls the calculation
--                       /prepare step (the daily SSBTEK loop). Null until
--                       the first loop has run. Lets Draken show "last
--                       checked" and lets ops spot errands whose loop has
--                       gone stale.
-- =====================================================================

alter table errand_financial_assistance add column last_daily_run_at datetime(6) null;
