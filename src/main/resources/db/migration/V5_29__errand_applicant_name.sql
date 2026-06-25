-- =====================================================================
-- Denormalized applicant name on the errand envelope, for sorting and
-- searching the errand list by applicant.
--
-- The errand envelope deliberately has no JPA relation to stakeholders
-- (separate module/table, joined only by errand_id), so the applicant
-- name is not otherwise reachable from an errand query. This column is a
-- read-model field maintained from the errand's APPLICANT stakeholder:
-- a type module recomputes it whenever that errand's stakeholders change
-- (see the financial-assistance ApplicantNameSyncListener). It stays null
-- for errand types that declare no applicant.
--
-- The composite index mirrors the existing (municipality_id, namespace, *)
-- indexes so `?sort=applicantName` over the always-present namespace +
-- municipality filter is index-backed.
-- =====================================================================

alter table errand add column applicant_name varchar(255);

create index idx_errand_municipality_namespace_applicant_name
    on errand (municipality_id, namespace, applicant_name);
