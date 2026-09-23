-- The applicant's open financial-assistance service (insats) id in Lifecare. Draken's BFF reads journal, documents,
-- reminders and jobbstimulans live from Lifecare, and those reads are keyed on the insats — so the errand carries the
-- key rather than every errand open looking it up in Lifecare again. Set at intake from the insats the actualisation
-- was linked to; filled in on read when the errand had none then (a nyansökan). A key only; no case data is kept.
ALTER TABLE `errand_financial_assistance`
  ADD COLUMN `lifecare_service_id` INT NULL AFTER `last_daily_run_at`;
