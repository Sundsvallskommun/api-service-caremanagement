-- Seed the EB (financial assistance) application-type display names into the generic lookup store
-- (kind = TYPE) for the Drakel tenant, so they are also queryable/editable via
-- GET/POST/PATCH /{municipalityId}/{namespace}/metadata?kind=TYPE.
--
-- The canonical, code-defined copy still ships read-only via /errand-types (built from
-- ErrandTypeContribution.displayName in FinancialAssistanceModuleConfig); these rows are the
-- runtime-editable mirror the verksamhet can override without a deploy. Keyed by the errand type
-- slug (the value stored on errand.type_slug), exactly as kind=STATUS is keyed by the status code.
-- Keep the two in sync if you change a default label in code.
--
-- INSERT IGNORE makes the migration idempotent against
-- uq_lookup_kind_namespace_municipality_id_name, so it is a no-op if a row already exists
-- (e.g. added via the metadata API before this migration ran).
insert ignore into lookup (kind, name, display_name, municipality_id, namespace, created) values
	('TYPE', 'financial-assistance-new',           'Nyansökan',       '2281', 'FINANCIAL_ASSISTANCE', now(6)),
	('TYPE', 'financial-assistance-renewal',        'Återansökan',     '2281', 'FINANCIAL_ASSISTANCE', now(6)),
	('TYPE', 'financial-assistance-supplementary',  'Tilläggsansökan', '2281', 'FINANCIAL_ASSISTANCE', now(6));
