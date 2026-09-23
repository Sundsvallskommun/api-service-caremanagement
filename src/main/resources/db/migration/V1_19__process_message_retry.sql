-- Processmeddelanden som inte nådde motorn (backlog: "Härda och slutverifiera processavslutet efter bifall", punkt 3).
-- finalize sparar beslutet även när korrelationen av PaymentDecisionReceived misslyckas; raden här skrivs i samma
-- transaktion som beslutet, så ett sparat beslut kan aldrig tyst lämna processen stående före beslutsgatewayen.
-- ProcessMessageRetryScheduler skickar om med backoff tills motorn tar emot meddelandet, och ger upp (status GAVE_UP,
-- loggas som fel) efter tre dygn. Variablerna är processvariablerna meddelandet bär, som JSON — inga personuppgifter.
CREATE TABLE `process_message_retry` (
  `id` varchar(36) NOT NULL,
  `errand_id` varchar(36) NOT NULL,
  `municipality_id` varchar(8) NOT NULL,
  `namespace` varchar(32) NOT NULL,
  `message_name` varchar(64) NOT NULL,
  `variables` varchar(1024) DEFAULT NULL,
  `status` varchar(16) NOT NULL,
  `attempts` int NOT NULL,
  `last_error` varchar(1024) DEFAULT NULL,
  `next_attempt` datetime(6) NOT NULL,
  `created` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_process_message_retry_status_next_attempt` (`status`,`next_attempt`),
  KEY `idx_process_message_retry_errand_id` (`errand_id`),
  CONSTRAINT `fk_process_message_retry_errand_id` FOREIGN KEY (`errand_id`) REFERENCES `errand` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
