-- Draken now reads the payees straight from Lifecare and creates new ones there directly, so the payee a caseworker
-- picks usually has no errand_financial_assistance_payee row — and that row was the only place a payment could get
-- the payee's Lifecare id from (V1_8). Without it, whoever registers the payment in Lifecare is left matching the
-- payee on name + account number, which is exactly what V1_8 set out to stop.
--
-- A plain copy, not a foreign key: the id is Lifecare's. Nullable, since a payment paying to a local payee row still
-- takes the id from that row.

ALTER TABLE `errand_financial_assistance_payment`
  ADD COLUMN `lifecare_payee_id` varchar(64) DEFAULT NULL AFTER `payee_id`;
