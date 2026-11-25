-- The payment rows finalize created before Draken registered payments directly in Lifecare (retired by V1_25) are no
-- longer read: payment-status verified them only for errands decided before that change, and on 2026-09-24 no process
-- instance was waiting on such an errand. The rows are not kept as history - the payments themselves are Lifecare's.
-- The Lifecare payment references of an errand live in errand_fa_lifecare_payment. errand_financial_assistance_payee
-- is a separate table and stays.
DROP TABLE IF EXISTS `errand_financial_assistance_payment_message_line`;
DROP TABLE IF EXISTS `errand_financial_assistance_payment_stakeholder`;
DROP TABLE IF EXISTS `errand_financial_assistance_payment`;
