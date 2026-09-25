-- The SSBTEK amount the normberäkning in Lifecare was last aligned with: what SSBTEK said when the system wrote the
-- income (the proposal, or a change Draken's BFF acknowledged). The SSBTEK_CALCULATION_DIFF warning is raised only
-- when SSBTEK has moved away from it, so a caseworker's own edit of the calculation no longer reads as an SSBTEK
-- change. Null for an income the system never wrote: every SSBTEK amount for it is news.
ALTER TABLE `errand_fa_calculation_sync`
  ADD COLUMN `ssbtek_baseline_amount` decimal(12,2) DEFAULT NULL AFTER `ssbtek_amount`;

-- An income the system wrote untouched was written at what SSBTEK said then. One the caseworker had already changed
-- in the proposal has no such record; today's reading is the best estimate there is.
UPDATE `errand_fa_calculation_sync`
   SET `ssbtek_baseline_amount` = COALESCE(`system_written_amount`, `ssbtek_amount`)
 WHERE `system_written_at` IS NOT NULL;
