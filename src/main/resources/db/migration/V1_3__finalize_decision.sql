-- "Besluta och utbetala": what the caseworker chose when finalizing is kept on the financial assistance errand for the
-- audit trail — the channels the decision is sent through (the Draken BFF does the sending) and whether the household
-- size (gemensamma kostnader) was changed in the draft, which the RPA WRITE_NORMBERAKNING item forwards to the robot so
-- it answers "Ja" to Lifecare's save-common-costs prompt. All nullable: null until the errand has been finalized.
ALTER TABLE `errand_financial_assistance`
  ADD COLUMN `household_size_changed` bit(1) DEFAULT NULL AFTER `last_daily_run_at`,
  ADD COLUMN `notify_mina_sidor` bit(1) DEFAULT NULL AFTER `household_size_changed`,
  ADD COLUMN `notify_digital_mailbox` bit(1) DEFAULT NULL AFTER `notify_mina_sidor`,
  ADD COLUMN `notify_letter` bit(1) DEFAULT NULL AFTER `notify_digital_mailbox`;
