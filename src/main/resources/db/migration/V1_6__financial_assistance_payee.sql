-- Betalningsmottagare added by hand on an errand, for the case where the applicant's Lifecare payment history does not
-- already contain the payee. Errand-scoped on purpose: the row is a bridging state until the ADD_PAYEE robot has put
-- the payee into Lifecare, after which the ordinary 12-month payment history carries it. No unique key on the payee
-- fields — the dedup is done in the service so an identical re-POST reuses the existing row rather than erroring.

CREATE TABLE `errand_financial_assistance_payee` (
  `id` varchar(36) NOT NULL,
  `errand_id` varchar(36) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `payment_method` varchar(64) DEFAULT NULL,
  `clearing` varchar(16) DEFAULT NULL,
  `account_number` varchar(64) DEFAULT NULL,
  `lifecare_status` varchar(16) NOT NULL DEFAULT 'PENDING',
  `lifecare_payee_id` varchar(64) DEFAULT NULL,
  `lifecare_detail` varchar(1024) DEFAULT NULL,
  `created` datetime(6) DEFAULT NULL,
  `modified` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_fa_payee_errand_id` (`errand_id`),
  CONSTRAINT `fk_fa_payee_errand_id` FOREIGN KEY (`errand_id`) REFERENCES `errand` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
