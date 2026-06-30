-- Decouple permit and referral from the core errand table, matching the V4_2 decoupling of the
-- other errand children (decision / stakeholder / attachment). The cross-module, non-cascading
-- errand FK (added in V4_5 / V4_6) otherwise throws on errand deletion before the asynchronous
-- {Permit,Referral}ErrandDeletedListener — driven by the ErrandDeleted event staged in Spring
-- Modulith's outbox — can remove the rows, making any errand that carries a permit or referral
-- undeletable. The idx_permit_errand_id / idx_referral_errand_id indexes remain, so the listeners'
-- deleteByErrandId stays indexed.
alter table if exists permit drop foreign key if exists fk_permit_errand_id;
alter table if exists referral drop foreign key if exists fk_referral_errand_id;
