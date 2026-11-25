-- The REGISTER_PAYMENT robot can now report back what Lifecare did (POST .../payments/{paymentId}/lifecare-result),
-- the counterpart of the ADD_PAYEE report that errand_financial_assistance_payee has had since V1_6. Until it existed
-- a decided payment stayed PENDING_REGISTRATION for ever, whatever the robot managed to do.
--
-- Only the failure text needs a new column: the Lifecare payment id goes into the existing lifecare_id, which was
-- already documented as "the payment's id in Lifecare once it exists there", and the outcome goes into status.
-- Same width as the payee row's lifecare_detail, and for the same reason — it is Lifecare's own message, shown to the
-- caseworker as-is.

ALTER TABLE `errand_financial_assistance_payment`
  ADD COLUMN `lifecare_detail` varchar(1024) DEFAULT NULL AFTER `lifecare_id`;
