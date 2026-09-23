-- The Lifecare decision (beslut) id the errand concerns. Set by the caseworker from Draken through the ordinary
-- PATCH .../financial-assistance/{errandId}/data, read back in the errand's data. A key only; no decision data is kept.
ALTER TABLE `errand_financial_assistance`
  ADD COLUMN `lifecare_decision_id` INT NULL AFTER `lifecare_service_id`;
