-- The Lifecare payments (utbetalningar) a bifall pays with. Draken's BFF registers each one directly in Lifecare from
-- the payment form and sets its Lifecare id on the errand through the ordinary PATCH .../financial-assistance/{errandId}/data,
-- the same way as lifecare_decision_id and lifecare_calculation_id. A reference only: whether a payment is registered
-- or paid out is read from Lifecare, never stored here. finalize requires at least one for a granting outcome, and
-- payment-status verifies exactly these ids against Lifecare.
--
-- The earlier path, where finalize created errand_fa_payment rows that were registered afterwards, is retired for new
-- decisions. errand_fa_payment and errand_fa_section_approval are deliberately kept, data included: errands decided
-- before this migration still have their payment-status checked against those rows, and both are audit history.
CREATE TABLE `errand_fa_lifecare_payment` (
  `errand_id` varchar(36) NOT NULL,
  `lifecare_payment_id` varchar(64) NOT NULL,
  PRIMARY KEY (`errand_id`, `lifecare_payment_id`),
  CONSTRAINT `fk_fa_lifecare_payment_errand_id` FOREIGN KEY (`errand_id`) REFERENCES `errand_financial_assistance` (`errand_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
