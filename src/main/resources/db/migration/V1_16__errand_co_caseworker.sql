-- Verksamhetens beslut (backlog/svar-verksamheten-2026-09-23.md punkt 4 "Medhandläggare och notiser"): en
-- handläggare ska kunna lägga till/ta bort medhandläggare på ett ärende i Draken. Både ordinarie handläggare
-- och medhandläggare ska få ärendets notifieringar, som ett och samma logiska meddelande — ingen
-- notifieringsrad dupliceras. `notification`-tabellen är oförändrad; den här tabellen ger bara
-- notifications-modulen ett extra villkor att bredda `owner_id`-sökningen mot.
CREATE TABLE `errand_co_caseworker` (
  `id` varchar(36) NOT NULL,
  `errand_id` varchar(36) NOT NULL,
  `municipality_id` varchar(8) NOT NULL,
  `namespace` varchar(32) NOT NULL,
  `user_id` varchar(64) NOT NULL,
  `created` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uc_co_caseworker_errand_id_user_id` (`errand_id`,`user_id`),
  KEY `idx_co_caseworker_errand_id` (`errand_id`),
  KEY `idx_co_caseworker_mid_ns_user_id` (`municipality_id`,`namespace`,`user_id`),
  CONSTRAINT `fk_co_caseworker_errand_id` FOREIGN KEY (`errand_id`) REFERENCES `errand` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
