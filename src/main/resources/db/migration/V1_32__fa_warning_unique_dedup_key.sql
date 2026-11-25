-- EXPENSE_CAPPED ("Kapad kostnad") is no longer raised: it repeated what the expense rule's EXPENSE_REVIEW text and the
-- decision tab's EXPENSE_PARTIALLY_REJECTED already say. Frozen drafts are never refreshed, so their rows would stay
-- open forever; they are derived data and go.
DELETE FROM `errand_financial_assistance_warning` WHERE `type` = 'EXPENSE_CAPPED';

-- Two reconciles of the same errand running at once could both find no warning and both insert it. Keep the oldest of
-- each (errand_id, type, source_key) and make the dedup key unique, so a concurrent insert becomes a no-op.
DELETE w
  FROM `errand_financial_assistance_warning` w
  JOIN `errand_financial_assistance_warning` k
    ON k.`errand_id` = w.`errand_id`
   AND k.`type` = w.`type`
   AND k.`source_key` = w.`source_key`
   AND (COALESCE(k.`created`, '1970-01-01') < COALESCE(w.`created`, '1970-01-01')
        OR (COALESCE(k.`created`, '1970-01-01') = COALESCE(w.`created`, '1970-01-01') AND k.`id` < w.`id`));

ALTER TABLE `errand_financial_assistance_warning`
  DROP INDEX `idx_fa_warning_dedup`,
  ADD UNIQUE KEY `uq_fa_warning_dedup` (`errand_id`, `type`, `source_key`);
