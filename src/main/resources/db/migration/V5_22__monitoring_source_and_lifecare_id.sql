-- =====================================================================
-- Bevakningar (monitorings): add provenance + a Lifecare back-reference so
-- they can be mirrored to/from Lifecare by RPA (the FC API carries no
-- bevakningar endpoint, so the sync is out-of-band).
--
--   source      — who authored the monitoring:
--                   CASEWORKER (created in Draken; RPA pushes it onto the
--                              person in Lifecare for the next application)
--                   LIFECARE   (read out of Lifecare by RPA and surfaced
--                              here on the errand)
--   lifecare_id — the monitoring's id in Lifecare once it exists there.
--                 Null for a caseworker row not yet mirrored; set once RPA
--                 has created it (so its presence marks "synced"), and the
--                 idempotency key RPA upserts LIFECARE-sourced rows on.
-- =====================================================================

alter table errand_financial_assistance_monitoring add column source varchar(16) not null default 'CASEWORKER';
alter table errand_financial_assistance_monitoring add column lifecare_id varchar(64) null;

create index idx_fa_monitoring_lifecare on errand_financial_assistance_monitoring (errand_id, lifecare_id);
