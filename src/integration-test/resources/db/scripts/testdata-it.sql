-- =========================================================================
-- Integration test seed data
-- Municipality id: 2281
-- Namespace: MY_NAMESPACE
-- =========================================================================

-- Lookup metadata (CONTACT_REASON, CATEGORY, STATUS, TYPE, ROLE)
INSERT INTO lookup (id, kind, namespace, municipality_id, name, display_name, created, modified) VALUES
    (1, 'CONTACT_REASON', 'MY_NAMESPACE', '2281', 'PHONE', 'Phone', '2025-01-01 12:00:00.000000', '2025-01-01 12:00:00.000000'),
    (2, 'CATEGORY',       'MY_NAMESPACE', '2281', 'CATEGORY-1', 'Category 1', '2025-01-01 12:00:00.000000', '2025-01-01 12:00:00.000000'),
    (3, 'STATUS',         'MY_NAMESPACE', '2281', 'NEW', 'New', '2025-01-01 12:00:00.000000', '2025-01-01 12:00:00.000000'),
    (4, 'TYPE',           'MY_NAMESPACE', '2281', 'TYPE-1', 'Type 1', '2025-01-01 12:00:00.000000', '2025-01-01 12:00:00.000000'),
    (5, 'ROLE',           'MY_NAMESPACE', '2281', 'APPLICANT', 'Applicant', '2025-01-01 12:00:00.000000', '2025-01-01 12:00:00.000000'),
    (6, 'STATUS',         'MY_NAMESPACE', '2281', 'OPEN', 'Open', '2025-01-01 12:00:00.000000', '2025-01-01 12:00:00.000000');

-- Namespace configurations
INSERT INTO namespace_config (id, municipality_id, namespace, display_name, short_code, created, modified) VALUES
    (1, '2281', 'MY_NAMESPACE',    'My namespace',    'MY',   '2025-01-01 12:00:00.000000', '2025-01-01 12:00:00.000000'),
    (2, '2281', 'OTHER_NAMESPACE', 'Other namespace', 'OTHR', '2025-01-01 12:00:00.000000', '2025-01-01 12:00:00.000000');

-- Errands
INSERT INTO errand (id, municipality_id, namespace, title, category, type, status, description, priority, reporter_user_id, assigned_user_id, contact_reason_id, contact_reason_description, created, modified, touched) VALUES
    ('11111111-1111-1111-1111-111111111111', '2281', 'MY_NAMESPACE', 'Errand one',   'CATEGORY-1', 'TYPE-1', 'NEW', 'First errand description',  'HIGH',   'reporter1', 'assignee1', 1, 'Caller said hello', '2025-01-02 09:00:00.000000', '2025-01-02 09:00:00.000000', '2025-01-02 09:00:00.000000'),
    ('22222222-2222-2222-2222-222222222222', '2281', 'MY_NAMESPACE', 'Errand two',   'CATEGORY-1', 'TYPE-1', 'NEW', 'Second errand description', 'MEDIUM', 'reporter2', 'assignee2', 1, NULL,                '2025-01-03 10:00:00.000000', '2025-01-03 10:00:00.000000', '2025-01-03 10:00:00.000000'),
    ('44444444-4444-4444-4444-444444444444', '2281', 'MY_NAMESPACE', 'Errand four',  'CATEGORY-1', 'TYPE-1', 'NEW', 'Fourth errand description', 'LOW',    'reporter4', NULL,        NULL, NULL,             '2025-01-04 11:00:00.000000', '2025-01-04 11:00:00.000000', '2025-01-04 11:00:00.000000');

-- External tags on errand one
INSERT INTO external_tag (errand_id, `key`, `value`) VALUES
    ('11111111-1111-1111-1111-111111111111', 'caseId', '12345');

-- Stakeholders
INSERT INTO stakeholder (id, errand_id, external_id, external_id_type, role, first_name, last_name, organization_name, address, care_of, zip_code, city, country, created, modified) VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '11111111-1111-1111-1111-111111111111', '198001011234', 'PRIVATE', 'APPLICANT', 'Joe',  'Doe', NULL, 'Storgatan 1', NULL, '85248', 'Sundsvall', 'Sweden', '2025-01-02 09:00:00.000000', '2025-01-02 09:00:00.000000'),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaab', '22222222-2222-2222-2222-222222222222', '198101011234', 'PRIVATE', 'APPLICANT', 'Jane', 'Doe', NULL, 'Storgatan 2', NULL, '85248', 'Sundsvall', 'Sweden', '2025-01-03 10:00:00.000000', '2025-01-03 10:00:00.000000');

-- Contact channels for the first stakeholder
INSERT INTO contact_channel (stakeholder_id, `key`, `value`) VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Email', 'joe.doe@example.com');

-- Errand parameters
INSERT INTO parameter (id, errand_id, display_name, parameter_group, parameters_key) VALUES
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '11111111-1111-1111-1111-111111111111', 'Priority level', 'contact', 'priorityLevel');

INSERT INTO parameter_values (parameter_id, value_order, value) VALUES
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 0, 'high'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 1, 'urgent');

-- Stakeholder parameters
INSERT INTO stakeholder_parameter (id, stakeholder_id, display_name, parameters_key) VALUES
    (100, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Phone number', 'phoneNumber');

INSERT INTO stakeholder_parameter_values (stakeholder_parameter_id, value) VALUES
    (100, '+46701234567');

-- Attachment data (file content) and attachment metadata
INSERT INTO attachment_data (id, file) VALUES
    (1, 0x48656c6c6f); -- "Hello"

INSERT INTO attachment (id, attachment_data_id, errand_id, file_name, mime_type, file_size, municipality_id, namespace, created, modified) VALUES
    ('dddddddd-dddd-dddd-dddd-dddddddddddd', 1, '11111111-1111-1111-1111-111111111111', 'hello.txt', 'text/plain', 5, '2281', 'MY_NAMESPACE', '2025-01-02 09:00:00.000000', '2025-01-02 09:00:00.000000');
