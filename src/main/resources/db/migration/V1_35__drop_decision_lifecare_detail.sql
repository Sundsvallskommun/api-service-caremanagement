-- Lifecare's refusal text for a decision is no longer kept. Finalize receipts the decision in-process, so nothing sets
-- lifecare_detail or the FAILED status any more (the PR #28 review, 593e8cf1). main folds this into its squashed V1_0;
-- this migration brings the drakel chain to the same schema.
ALTER TABLE `decision` DROP COLUMN IF EXISTS `lifecare_detail`;
