-- =========================================================================
-- Lifecare ProfessionalWeb seed: one återansökan with its insats known (lifecare_service_id) and no beslut saved in
-- Lifecare yet, so saving the beslut creates it.
-- No real personal data: the party id is synthetic.
-- =========================================================================

INSERT INTO errand (id, municipality_id, namespace, title, type_slug, status, description, priority, reporter_user_id, assigned_user_id, process_definition_name, process_instance_id, created, modified, touched) VALUES
    ('88888888-8888-8888-8888-888888888888', '2281', 'MY_NAMESPACE', 'Errand eight', 'financial-assistance-renewal', 'AWAITING_DECISION', 'Återansökan without a beslut in Lifecare', 'MEDIUM', 'reporter8', 'assignee8', NULL, NULL, '2025-01-08 09:00:00.000000', '2025-01-08 09:00:00.000000', '2025-01-08 09:00:00.000000');

INSERT INTO errand_financial_assistance (errand_id, application_type, lifecare_service_id) VALUES
    ('88888888-8888-8888-8888-888888888888', 'RENEWAL', 8800);

INSERT INTO errand_fa_person (errand_id, role, party_id) VALUES
    ('88888888-8888-8888-8888-888888888888', 'APPLICANT', 'aaaaaaaa-0000-4000-8000-000000000008');
