-- RPA retired 2026-09-24: Draken's BFF reads journal notes, documents and bevakningar directly from Lifecare, and the
-- supplements ingest that mirrored them onto the errand is gone. The mirrored rows are Lifecare records about the
-- applicant that careM no longer serves or refreshes, so they are removed rather than left to go stale.
-- Caseworker-authored rows (source CASEWORKER) are untouched. The jobbstimulans periods stay until Draken reads them
-- live; errand_fa_job_stimulus_period is not touched here.

DELETE FROM `errand_journal_entry` WHERE `source` = 'LIFECARE';
DELETE FROM `errand_document` WHERE `source` = 'LIFECARE';
DELETE FROM `errand_financial_assistance_monitoring` WHERE `source` = 'LIFECARE';
