-- jobStimulusAmount on the normberäkning draft's person rows was stored and returned but never read: it was not
-- deducted in the draft, not sent to Lifecare and not shown by Draken. Lifecare makes the jobbstimulans deduction in
-- the calculation Draken saves there, and Draken reads the periods from Lifecare, so the field is removed from the
-- API and the column is dropped.
ALTER TABLE `errand_fa_norm_person`
  DROP COLUMN IF EXISTS `job_stimulus_amount`;
