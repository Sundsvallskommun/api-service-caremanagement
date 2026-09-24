SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE attachment;
TRUNCATE TABLE attachment_data;
TRUNCATE TABLE contact_channel;
TRUNCATE TABLE decision;
TRUNCATE TABLE errand_document;
-- The access log was missing here: every AppTest request writes rows to it, so without this they accumulate across
-- ITs. Harmless while nothing asserted on them; not harmless now that a search writes one row per hit.
TRUNCATE TABLE errand_co_caseworker;
TRUNCATE TABLE errand_event;
-- The financial-assistance child tables have to be listed too: with the foreign key checks off, truncating the parent
-- does not cascade, so a seed that inserts persons or costs would otherwise pile up duplicates run after run.
TRUNCATE TABLE errand_fa_asset;
TRUNCATE TABLE errand_fa_calculation_draft_norm_type;
TRUNCATE TABLE errand_fa_child;
TRUNCATE TABLE errand_fa_cost;
TRUNCATE TABLE errand_fa_income;
TRUNCATE TABLE errand_fa_job_application;
TRUNCATE TABLE errand_fa_lifecare_payment;
TRUNCATE TABLE errand_fa_norm_expense;
TRUNCATE TABLE errand_fa_norm_income;
TRUNCATE TABLE errand_fa_norm_person;
TRUNCATE TABLE errand_fa_norm_type;
TRUNCATE TABLE errand_fa_pending_benefit;
TRUNCATE TABLE errand_fa_person;
TRUNCATE TABLE errand_fa_planned_activity;
TRUNCATE TABLE errand_fa_planning;
TRUNCATE TABLE errand_financial_assistance;
TRUNCATE TABLE errand_financial_assistance_calculation_draft;
TRUNCATE TABLE errand_financial_assistance_monitoring;
TRUNCATE TABLE errand_financial_assistance_payee;
TRUNCATE TABLE errand_financial_assistance_warning;
TRUNCATE TABLE errand_form_snapshot;
TRUNCATE TABLE errand_message;
TRUNCATE TABLE errand_note;
TRUNCATE TABLE errand_status_history;
TRUNCATE TABLE message_attachment;
TRUNCATE TABLE message_attachment_data;
TRUNCATE TABLE message_read_receipt;
TRUNCATE TABLE notification;
TRUNCATE TABLE process_message_retry;
TRUNCATE TABLE stakeholder;
TRUNCATE TABLE errand;
TRUNCATE TABLE errand_number_sequence;
TRUNCATE TABLE lookup;
TRUNCATE TABLE namespace_config;
TRUNCATE TABLE shedlock;
SET FOREIGN_KEY_CHECKS = 1;
