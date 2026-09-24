-- Per-user settings for the handläggare frontend, keyed on the AD account (stored lower case). A user without a row
-- runs on the defaults, so the column default mirrors the service default (open SSBTEK in a new window).
CREATE TABLE `user_settings` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `municipality_id` varchar(8) NOT NULL,
  `ad_account` varchar(64) NOT NULL,
  `ssbtek_open_in_new_window` bit(1) NOT NULL DEFAULT b'1',
  `created` datetime(6) DEFAULT NULL,
  `modified` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_user_settings_municipality_id_ad_account` (`municipality_id`,`ad_account`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
