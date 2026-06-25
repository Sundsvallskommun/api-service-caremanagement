-- Seed the EB (financial assistance) status display names into the generic lookup store
-- (kind = STATUS) for the Drakel tenant, so they are also queryable/editable via
-- GET/POST/PATCH /{municipalityId}/{namespace}/metadata?kind=STATUS.
--
-- The canonical, code-defined copy still ships read-only via /errand-types (built from
-- ErrandTypeContribution); these rows are the runtime-editable mirror the verksamhet can
-- override without a deploy. Keep the two in sync if you change a default label in code.
--
-- INSERT IGNORE makes the migration idempotent against
-- uq_lookup_kind_namespace_municipality_id_name, so it is a no-op if a row already exists
-- (e.g. added via the metadata API before this migration ran).
insert ignore into lookup (kind, name, display_name, municipality_id, namespace, created) values
	('STATUS', 'RECEIVED',             'Inkommen',                  '2281', 'FINANCIAL_ASSISTANCE', now(6)),
	('STATUS', 'NEEDS_MANUAL_REVIEW',  'Kräver manuell granskning', '2281', 'FINANCIAL_ASSISTANCE', now(6)),
	('STATUS', 'UNDER_REVIEW',         'Under utredning',           '2281', 'FINANCIAL_ASSISTANCE', now(6)),
	('STATUS', 'SUPPLEMENT_REQUESTED', 'Komplettering begärd',      '2281', 'FINANCIAL_ASSISTANCE', now(6)),
	('STATUS', 'AWAITING_DECISION',    'Väntar på beslut',          '2281', 'FINANCIAL_ASSISTANCE', now(6)),
	('STATUS', 'GRANTED',              'Beviljad',                  '2281', 'FINANCIAL_ASSISTANCE', now(6)),
	('STATUS', 'REJECTED',             'Avslagen',                  '2281', 'FINANCIAL_ASSISTANCE', now(6)),
	('STATUS', 'PAID',                 'Utbetald',                  '2281', 'FINANCIAL_ASSISTANCE', now(6)),
	('STATUS', 'WITHDRAWN',            'Återtagen',                 '2281', 'FINANCIAL_ASSISTANCE', now(6)),
	('STATUS', 'CLOSED',               'Avslutad',                  '2281', 'FINANCIAL_ASSISTANCE', now(6));
