-- Draken's BFF owns the normberäkning in Lifecare: the caseworker saves it from Draken before the decision (bifall
-- takes its amount from it), and careM no longer creates it. The id of that calculation is set on the errand through
-- the ordinary PATCH .../financial-assistance/{errandId}/data and read back in the errand's data. A key only; no
-- calculation data is kept. While it is set, the daily prepare no longer refreshes careM's calculation draft.
ALTER TABLE `errand_financial_assistance`
  ADD COLUMN `lifecare_calculation_id` INT NULL AFTER `lifecare_decision_id`;

-- Draken reads the jobbstimulans periods directly from Lifecare, so careM's copy (created in V1_1, kept by V1_21
-- until Draken read them live) is dropped.
DROP TABLE IF EXISTS `errand_fa_job_stimulus_period`;
