-- RPA supplements ingest: Lifecare provenance on journal entries and documents (mirrors the monitorings pattern),
-- plus the jobbstimulans period mirror table.

ALTER TABLE `errand_journal_entry`
  ADD COLUMN `source` varchar(16) DEFAULT NULL,
  ADD COLUMN `lifecare_id` varchar(64) DEFAULT NULL,
  ADD UNIQUE KEY `uq_journal_entry_errand_id_lifecare_id` (`errand_id`, `lifecare_id`);

ALTER TABLE `errand_document`
  ADD COLUMN `source` varchar(16) DEFAULT NULL,
  ADD COLUMN `lifecare_id` varchar(64) DEFAULT NULL,
  ADD UNIQUE KEY `uq_document_errand_id_lifecare_id` (`errand_id`, `lifecare_id`);

-- Rows written before provenance existed were all authored in Draken.
UPDATE `errand_journal_entry` SET `source` = 'CASEWORKER' WHERE `source` IS NULL;
UPDATE `errand_document` SET `source` = 'CASEWORKER' WHERE `source` IS NULL;

CREATE TABLE `errand_fa_job_stimulus_period` (
  `id` varchar(36) NOT NULL,
  `errand_id` varchar(36) NOT NULL,
  `role` varchar(16) DEFAULT NULL,
  `from_date` date DEFAULT NULL,
  `to_date` date DEFAULT NULL,
  `created` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_fa_job_stimulus_period_errand_id` (`errand_id`),
  CONSTRAINT `fk_fa_job_stimulus_period_errand_id` FOREIGN KEY (`errand_id`) REFERENCES `errand` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
