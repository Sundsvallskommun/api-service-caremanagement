-- =========================================================================
-- Withdrawal seed data: one financial assistance renewal errand waiting for a decision. Withdrawing it (PATCH status
-- WITHDRAWN) must end its process. Envelope row only: the listener works from the type slug, and the process message
-- correlation checks that the errand exists.
-- Kept out of testdata-it.sql so the errand lists the other ITs assert on stay as they are.
-- =========================================================================

INSERT INTO errand (id, municipality_id, namespace, title, type_slug, status, description, priority, reporter_user_id, assigned_user_id, process_definition_name, process_instance_id, created, modified, touched) VALUES
    ('77777777-7777-7777-7777-777777777777', '2281', 'MY_NAMESPACE', 'Errand seven', 'financial-assistance-renewal', 'AWAITING_DECISION', 'Errand to withdraw', 'MEDIUM', 'reporter7', 'assignee7', NULL, NULL, '2025-01-07 09:00:00.000000', '2025-01-07 09:00:00.000000', '2025-01-07 09:00:00.000000');
