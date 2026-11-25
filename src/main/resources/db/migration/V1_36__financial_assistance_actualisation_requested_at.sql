-- The Lifecare actualisation step records, before it calls Lifecare, that it is about to create an actualisation.
-- FamilyCare has no delete and no lookup by errand, so a retry that cannot tell whether an earlier attempt already
-- created the actualisation would create a second one. The column is set in a transaction of its own, so it survives
-- whatever happens to the intake's transaction after the create; a set column with no recorded actualisation id is what
-- tells the retry to look in Lifecare for the earlier attempt's actualisation before creating another.
--
-- Set once, by the first attempt, and never cleared: it says when the step first went to Lifecare for the errand.

ALTER TABLE `errand_financial_assistance`
  ADD COLUMN `actualisation_requested_at` datetime(6) DEFAULT NULL;
