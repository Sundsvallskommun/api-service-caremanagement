-- The section approvals were Draken's "Markera som komplett" check-offs for the calculation, payment and decision
-- sections. They stopped gating finalize with V1_25 and stopped driving anything in careM after that. Draken now takes
-- the tab check marks from Lifecare (Drakel bfe1f5b) and no longer calls the approval endpoints, so the endpoints, the
-- view field and the table are removed. The rows are not kept.
DROP TABLE IF EXISTS `errand_financial_assistance_section_approval`;
