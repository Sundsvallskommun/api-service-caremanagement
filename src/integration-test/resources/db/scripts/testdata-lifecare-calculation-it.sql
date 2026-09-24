-- =========================================================================
-- Lifecare-calculation contract seed: one återansökan ready for "Besluta och utbetala" — AWAITING_DECISION, all
-- three sections approved, no PAYMENT decision, and NO lifecare_calculation_id yet (Draken has not saved the
-- normberäkning in Lifecare). The applicant and the rent are what the draft refresh builds its rows from.
-- No real personal data: the party id is synthetic and the citizen stub answers with a made-up personnummer.
-- =========================================================================

INSERT INTO errand (id, municipality_id, namespace, title, type_slug, status, description, priority, reporter_user_id, assigned_user_id, process_definition_name, process_instance_id, created, modified, touched) VALUES
    ('77777777-7777-7777-7777-777777777777', '2281', 'MY_NAMESPACE', 'Errand seven', 'financial-assistance-renewal', 'AWAITING_DECISION', 'Återansökan ready for a decision', 'MEDIUM', 'reporter7', 'assignee7', NULL, NULL, '2025-01-07 09:00:00.000000', '2025-01-07 09:00:00.000000', '2025-01-07 09:00:00.000000');

INSERT INTO errand_financial_assistance (errand_id, application_type) VALUES
    ('77777777-7777-7777-7777-777777777777', 'RENEWAL');

INSERT INTO errand_fa_person (errand_id, role, party_id) VALUES
    ('77777777-7777-7777-7777-777777777777', 'APPLICANT', 'aaaaaaaa-0000-4000-8000-000000000001');

INSERT INTO errand_fa_cost (errand_id, cost_type, applied_amount) VALUES
    ('77777777-7777-7777-7777-777777777777', 'RENT', 6500.00);

INSERT INTO errand_financial_assistance_section_approval (id, errand_id, section, approved, created) VALUES
    ('77777777-aaaa-aaaa-aaaa-000000000001', '77777777-7777-7777-7777-777777777777', 'CALCULATION', b'1', '2025-01-07 09:10:00.000000'),
    ('77777777-aaaa-aaaa-aaaa-000000000002', '77777777-7777-7777-7777-777777777777', 'PAYMENT', b'1', '2025-01-07 09:10:00.000000'),
    ('77777777-aaaa-aaaa-aaaa-000000000003', '77777777-7777-7777-7777-777777777777', 'DECISION', b'1', '2025-01-07 09:10:00.000000');
