-- What careM knows about each income of a normberäkning saved in Lifecare: the latest SSBTEK amount the daily prepare
-- computed for it, and the amount the system last wrote to Lifecare (the proposal, or a change Draken's BFF applied
-- and acknowledged). One row per errand, Lifecare income type and role. Comparing the two against the calculation
-- in Lifecare tells an SSBTEK change apart from a caseworker's edit. Hangs off the calculation draft, so finalize's
-- draft purge removes it too.
CREATE TABLE `errand_fa_calculation_sync` (
  `id` varchar(36) NOT NULL,
  `errand_id` varchar(36) NOT NULL,
  `income_type_key` varchar(255) NOT NULL,
  `income_type_id` int(11) DEFAULT NULL,
  `income_type_name` varchar(255) DEFAULT NULL,
  `role` varchar(16) NOT NULL,
  `ssbtek_amount` decimal(12,2) DEFAULT NULL,
  `ssbtek_read_at` datetime(6) DEFAULT NULL,
  `system_written_amount` decimal(12,2) DEFAULT NULL,
  `system_written_at` datetime(6) DEFAULT NULL,
  `created` datetime(6) DEFAULT NULL,
  `updated` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_fa_calculation_sync_errand_type_role` (`errand_id`,`income_type_key`,`role`),
  CONSTRAINT `fk_fa_calculation_sync_draft` FOREIGN KEY (`errand_id`) REFERENCES `errand_financial_assistance_calculation_draft` (`errand_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
