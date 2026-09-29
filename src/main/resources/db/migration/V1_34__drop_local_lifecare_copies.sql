-- careM's local copies of Lifecare records are retired: journal notes, documents, payees and bevakningar are read and
-- written in Lifecare through careM's /lifecare routes, and careM keeps no copy of them. The endpoints that filled these
-- tables are removed; all four tables were empty in Drakel on 2026-09-29. The journal and document type catalogues
-- stay; they live in the metadata lookup store, not here.
DROP TABLE IF EXISTS `errand_journal_entry`;
DROP TABLE IF EXISTS `errand_document`;
DROP TABLE IF EXISTS `errand_financial_assistance_payee`;
DROP TABLE IF EXISTS `errand_financial_assistance_monitoring`;
