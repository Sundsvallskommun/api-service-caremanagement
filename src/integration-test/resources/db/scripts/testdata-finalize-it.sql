-- =========================================================================
-- Finalize seed data: one financial assistance errand ready for "Besluta och utbetala" —
-- AWAITING_DECISION, no PAYMENT decision yet, the beslut (lifecare_decision_id) and the normberäkning saved in Lifecare
-- (lifecare_calculation_id set), which a granting decision requires.
-- Kept out of testdata-it.sql so the errand lists the other ITs assert on stay as they are.
-- =========================================================================

INSERT INTO errand (id, municipality_id, namespace, title, type_slug, status, description, priority, reporter_user_id, assigned_user_id, process_definition_name, process_instance_id, created, modified, touched) VALUES
    ('66666666-6666-6666-6666-666666666666', '2281', 'MY_NAMESPACE', 'Errand six', 'TYPE-1', 'AWAITING_DECISION', 'Errand ready for a decision', 'MEDIUM', 'reporter6', 'assignee6', NULL, NULL, '2025-01-06 09:00:00.000000', '2025-01-06 09:00:00.000000', '2025-01-06 09:00:00.000000');

INSERT INTO errand_financial_assistance (errand_id, lifecare_calculation_id, lifecare_decision_id) VALUES
    ('66666666-6666-6666-6666-666666666666', 4711, 815);
