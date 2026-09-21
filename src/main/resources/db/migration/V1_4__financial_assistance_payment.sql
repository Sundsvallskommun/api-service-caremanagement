-- Utbetalningar (Payment resource). These three tables were originally added straight into the V1_0 baseline, which
-- had already been applied to the Drakel database — that changed its checksum and crash-looped the service on Flyway
-- validation. The baseline is restored to its applied state and the tables live here instead. Content is unchanged.

CREATE TABLE `errand_financial_assistance_payment` (
  `id` varchar(36) NOT NULL,
  `errand_id` varchar(36) NOT NULL,
  `source` varchar(16) NOT NULL DEFAULT 'CASEWORKER',
  `lifecare_id` varchar(64) DEFAULT NULL,
  `status` varchar(16) NOT NULL DEFAULT 'DRAFT',
  `money_type` varchar(64) DEFAULT NULL,
  `payment_date` date DEFAULT NULL,
  `amount` decimal(15,2) DEFAULT NULL,
  `application_month` varchar(7) DEFAULT NULL,
  `accounting_date` date DEFAULT NULL,
  `excluded_from_payment` bit(1) NOT NULL DEFAULT b'0',
  `payee_stakeholder_id` varchar(64) DEFAULT NULL,
  `payment_method` varchar(64) DEFAULT NULL,
  `payee_name` varchar(255) DEFAULT NULL,
  `payee_address` varchar(255) DEFAULT NULL,
  `payee_care_of` varchar(255) DEFAULT NULL,
  `payee_zip_code` varchar(16) DEFAULT NULL,
  `payee_city` varchar(255) DEFAULT NULL,
  `clearing_number` varchar(64) DEFAULT NULL,
  `account_number` varchar(64) DEFAULT NULL,
  `local_payment_number` varchar(64) DEFAULT NULL,
  `invoice_number` varchar(64) DEFAULT NULL,
  `uses_ocr` bit(1) NOT NULL DEFAULT b'0',
  `created` datetime(6) DEFAULT NULL,
  `modified` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  -- No unique key on (errand_id, application_month) — several payments per application month are allowed.
  UNIQUE KEY `uq_fa_payment_errand_id_lifecare_id` (`errand_id`,`lifecare_id`),
  CONSTRAINT `fk_fa_payment_errand_id` FOREIGN KEY (`errand_id`) REFERENCES `errand` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `errand_financial_assistance_payment_stakeholder` (
  `payment_id` varchar(36) NOT NULL,
  `stakeholder_id` varchar(64) DEFAULT NULL,
  KEY `idx_fa_payment_stakeholder_payment_id` (`payment_id`),
  CONSTRAINT `fk_fa_payment_stakeholder_payment_id` FOREIGN KEY (`payment_id`) REFERENCES `errand_financial_assistance_payment` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `errand_financial_assistance_payment_message_line` (
  `payment_id` varchar(36) NOT NULL,
  `message_line` varchar(255) DEFAULT NULL,
  KEY `idx_fa_payment_message_line_payment_id` (`payment_id`),
  CONSTRAINT `fk_fa_payment_message_line_payment_id` FOREIGN KEY (`payment_id`) REFERENCES `errand_financial_assistance_payment` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
