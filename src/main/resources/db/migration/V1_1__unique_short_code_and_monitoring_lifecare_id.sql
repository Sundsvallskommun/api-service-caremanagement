-- A duplicated (errand_id, lifecare_id) pair would break the RPA upsert lookup (it expects at most one row),
-- and a duplicated short code within a municipality would mint colliding errand numbers. Enforce both in the
-- database. Deduplicate first so the constraints can be added on environments where duplicates already slipped
-- in (both statements are no-ops on clean data).

-- Monitorings: keep the most recently created row per (errand_id, lifecare_id).
DELETE m1 FROM errand_financial_assistance_monitoring m1
JOIN errand_financial_assistance_monitoring m2
  ON m1.errand_id = m2.errand_id
  AND m1.lifecare_id = m2.lifecare_id
  AND (m1.created < m2.created OR (m1.created = m2.created AND m1.id < m2.id));

ALTER TABLE errand_financial_assistance_monitoring
  DROP INDEX idx_fa_monitoring_lifecare,
  ADD UNIQUE KEY uq_fa_monitoring_errand_id_lifecare_id (errand_id, lifecare_id);

-- Namespace short codes: clear the later duplicates within a municipality (their errand number prefix falls
-- back to the namespace name until an operator assigns a new, unique short code).
UPDATE namespace_config nc
JOIN namespace_config keeper
  ON keeper.municipality_id = nc.municipality_id
  AND keeper.short_code = nc.short_code
  AND keeper.id < nc.id
SET nc.short_code = NULL;

ALTER TABLE namespace_config
  ADD UNIQUE KEY uq_namespace_config_municipality_id_short_code (municipality_id, short_code);
