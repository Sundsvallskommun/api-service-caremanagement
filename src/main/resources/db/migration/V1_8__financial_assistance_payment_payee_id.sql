-- The payment carried the payee's name, method, clearing and account number as copied text, and nothing else — so the
-- row it came from was unreachable. That row (errand_financial_assistance_payee) is the only place lifecarePayeeId
-- lives, the id the ADD_PAYEE robot reports back after putting the payee into Lifecare. Without this column the
-- REGISTER_PAYMENT robot had to match the payee in Lifecare on name + account number, which fails silently.
--
-- Nullable, and stays nullable: a payee derived from the 12-month Lifecare history has no local row at all, and a
-- caseworker may delete a manual payee after the decision. ON DELETE SET NULL rather than CASCADE — the payment's own
-- copied fields are owned by the decision and must survive the payee row going away.

ALTER TABLE `errand_financial_assistance_payment`
  ADD COLUMN `payee_id` varchar(36) DEFAULT NULL AFTER `payee_stakeholder_id`,
  ADD KEY `idx_fa_payment_payee_id` (`payee_id`),
  ADD CONSTRAINT `fk_fa_payment_payee_id` FOREIGN KEY (`payee_id`)
    REFERENCES `errand_financial_assistance_payee` (`id`) ON DELETE SET NULL;
