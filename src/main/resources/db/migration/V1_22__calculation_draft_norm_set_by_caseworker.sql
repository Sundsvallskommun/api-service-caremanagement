-- The daily beredning rewrote the draft's norm_id on every run, so a norm the caseworker had picked in Draken was put
-- back to the process's choice the next night. The flag records that the caseworker chose it; the beredning then
-- leaves norm_id alone. Existing drafts carry no record of who set the norm, so they start as the process's.

ALTER TABLE `errand_financial_assistance_calculation_draft`
  ADD COLUMN `norm_set_by_caseworker` bit(1) DEFAULT NULL;
