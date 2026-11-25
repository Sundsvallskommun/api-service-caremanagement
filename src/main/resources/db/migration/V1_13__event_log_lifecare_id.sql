-- Draken's BFF reads and writes Lifecare directly (journal, documents, reminders, jobbstimulans) and reports each
-- access here, so the access log still answers "who saw or wrote what on this errand" — verksamhetens regelverk
-- (revision 2026-09-22). lifecare_id is the Lifecare record the access was about: which journal note was written,
-- which document was opened. Null for a list read and for every access that goes through careM itself.
ALTER TABLE `errand_event`
  ADD COLUMN `lifecare_id` VARCHAR(64) NULL AFTER `request_path`;
