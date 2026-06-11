-- =====================================================================
-- Phase 2a — kill the parameter swamp.
--
-- Drops parameter / stakeholder_parameter / external_tag tables, removes
-- swamp columns from errand, adds errand_number, renames type → type_slug.
--
-- Does NOT preserve existing parameter/externalTag data — the swamp had no
-- contract worth preserving. If you need to migrate prod data, write an ETL
-- before running this script.
-- =====================================================================

-- -- Drop parameter tables (and any value sub-tables) ----------------------
-- Child tables first (they hold FKs to the parent tables).
DROP TABLE IF EXISTS stakeholder_parameter_values;
DROP TABLE IF EXISTS stakeholder_parameter;
DROP TABLE IF EXISTS parameter_values;
DROP TABLE IF EXISTS parameter;
DROP TABLE IF EXISTS external_tag;

-- -- Slim down errand ------------------------------------------------------
-- Drop dead indexes first so renames don't conflict.
ALTER TABLE errand DROP INDEX IF EXISTS idx_errand_municipality_id_namespace_category;
ALTER TABLE errand DROP INDEX IF EXISTS idx_errand_municipality_id_namespace_type;
ALTER TABLE errand DROP INDEX IF EXISTS idx_errand_municipality_id_namespace_status;
ALTER TABLE errand DROP INDEX IF EXISTS idx_errand_municipality_id_namespace_assigned_user_id;
ALTER TABLE errand DROP INDEX IF EXISTS idx_errand_municipality_id_namespace_reporter_user_id;
ALTER TABLE errand DROP INDEX IF EXISTS idx_errand_municipality_id_namespace_status_touched;
ALTER TABLE errand DROP INDEX IF EXISTS idx_errand_municipality_id_namespace_status_modified;
ALTER TABLE errand DROP INDEX IF EXISTS idx_errand_municipality_id_namespace_created;
ALTER TABLE errand DROP INDEX IF EXISTS idx_errand_municipality_id_namespace_touched;

-- Drop foreign-key to lookup (contact_reason) since we're removing the columns.
-- (Constraint name may differ — adjust if your environment uses a different one.)
ALTER TABLE errand DROP FOREIGN KEY IF EXISTS fk_errand_contact_reason_id;

ALTER TABLE errand DROP COLUMN IF EXISTS category;
ALTER TABLE errand DROP COLUMN IF EXISTS contact_reason_id;
ALTER TABLE errand DROP COLUMN IF EXISTS contact_reason_description;

-- Rename type → type_slug
ALTER TABLE errand CHANGE COLUMN type type_slug VARCHAR(64);

-- Add errand_number (nullable for existing rows; service generates on create going forward).
ALTER TABLE errand ADD COLUMN errand_number VARCHAR(64);
ALTER TABLE errand ADD CONSTRAINT uq_errand_errand_number UNIQUE (errand_number);

-- Rebuild useful indexes
CREATE INDEX idx_errand_errand_number                       ON errand (errand_number);
CREATE INDEX idx_errand_municipality_namespace_type_slug    ON errand (municipality_id, namespace, type_slug);
CREATE INDEX idx_errand_municipality_namespace_status       ON errand (municipality_id, namespace, status);
CREATE INDEX idx_errand_municipality_namespace_assigned_user_id ON errand (municipality_id, namespace, assigned_user_id);
CREATE INDEX idx_errand_municipality_namespace_reporter_user_id ON errand (municipality_id, namespace, reporter_user_id);
CREATE INDEX idx_errand_municipality_namespace_status_touched   ON errand (municipality_id, namespace, status, touched);
CREATE INDEX idx_errand_municipality_namespace_created      ON errand (municipality_id, namespace, created);
CREATE INDEX idx_errand_municipality_namespace_touched      ON errand (municipality_id, namespace, touched);
