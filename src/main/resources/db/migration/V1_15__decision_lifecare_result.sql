-- Whether a decision made it into Lifecare. Finalize hands the payment decision over to be written there (by the
-- robot today, possibly by Draken's BFF); until now careM never learned whether that write succeeded, so a refused
-- decision was invisible. The writer reports back through POST .../decisions/{id}/lifecare-result.
ALTER TABLE `decision`
  ADD COLUMN `lifecare_status` VARCHAR(16) NULL AFTER `created_by`,
  ADD COLUMN `lifecare_id` VARCHAR(64) NULL AFTER `lifecare_status`,
  ADD COLUMN `lifecare_detail` VARCHAR(1024) NULL AFTER `lifecare_id`;
