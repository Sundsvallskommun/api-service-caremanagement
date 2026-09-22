-- PENDING_REGISTRATION is 20 characters and status was varchar(16), so every finalize carrying a payment failed with
-- "Data too long for column 'status'" — the row could not be written at all. Found by running finalize against Drakel;
-- the service tests mock the repository, so the column width never took part.
--
-- Widened rather than shortening the value: 16 was an arbitrary bound, and the status vocabulary should be free to say
-- what a row is. source stays 16 (CASEWORKER/LIFECARE).

ALTER TABLE `errand_financial_assistance_payment`
  MODIFY COLUMN `status` varchar(32) NOT NULL DEFAULT 'DRAFT';
