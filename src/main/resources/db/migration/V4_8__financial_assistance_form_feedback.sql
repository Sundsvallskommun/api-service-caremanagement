-- =====================================================================
-- Återansökan form feedback from verksamheten (Rakel 2.0):
--   * Housing is captured as a single household person count, no longer
--     split into adults vs children.
--   * Sick-leave from/to dates dropped from planning (level is kept).
-- V4_7 created these columns; this migration evolves them (V4_7 is
-- already deployed, so it cannot be edited in place).
-- =====================================================================

alter table errand_financial_assistance
    drop column housing_adults_count,
    drop column housing_children_count,
    add column housing_person_count int;

alter table errand_fa_planning
    drop column sick_from,
    drop column sick_to;
