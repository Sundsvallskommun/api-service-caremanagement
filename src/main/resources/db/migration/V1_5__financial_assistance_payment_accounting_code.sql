-- Kontering on a payment. The finalize step sends the caseworker's accounting code with the payment, and until now
-- the Payment resource had nowhere to put it — so finalize could only pass it inline on the RPA queue item, which is
-- exactly what the paymentId-only queue contract removes.
--
-- Free text, deliberately: FamilyCare exposes no catalogue of accounting codes over the API, and verksamheten has not
-- given us the value set. See the open question in the sprint backlog.

ALTER TABLE `errand_financial_assistance_payment`
  ADD COLUMN `accounting_code` varchar(64) DEFAULT NULL AFTER `application_month`;
