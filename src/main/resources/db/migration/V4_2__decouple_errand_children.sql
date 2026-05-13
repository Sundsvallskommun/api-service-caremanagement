-- =====================================================================
-- Decouple errand from child modules. The JPA cycle was already broken
-- in phase 2c — children carry a plain {@code errand_id} String with no
-- @ManyToOne back-reference. This migration removes the matching DB-level
-- FK constraints from the legacy V1_0 schema so modules truly own their
-- own data. Cleanup on errand-delete is handled by @ApplicationModuleListener
-- on ErrandDeleted in each child module's service.
--
-- Intra-module FKs (attachment_data ↔ attachment, contact_channel ↔ stakeholder)
-- get ON DELETE CASCADE so deleting the parent row inside a module still
-- cleans up its own children atomically.
--
-- Notification / errand_note / errand_status_history already cascade against
-- errand from their own migrations (V1_3 / V4_0 / V4_1) — left as-is.
-- =====================================================================

-- attachment -> errand: drop, replaced by AttachmentService ErrandDeleted listener.
alter table if exists attachment drop foreign key if exists fk_errand_attachment_errand_id;

-- attachment_data -> attachment: intra-module, ensure ON DELETE CASCADE.
alter table if exists attachment drop foreign key if exists fk_attachment_data_attachment;
alter table if exists attachment
    add constraint fk_attachment_data_attachment
    foreign key (attachment_data_id)
    references attachment_data (id)
    on delete cascade;

-- stakeholder -> errand: drop, replaced by StakeholderService ErrandDeleted listener.
alter table if exists stakeholder drop foreign key if exists fk_stakeholder_errand_id;

-- contact_channel -> stakeholder: intra-module, ensure ON DELETE CASCADE.
alter table if exists contact_channel drop foreign key if exists fk_stakeholder_contact_channel_stakeholder_id;
alter table if exists contact_channel
    add constraint fk_stakeholder_contact_channel_stakeholder_id
    foreign key (stakeholder_id)
    references stakeholder (id)
    on delete cascade;

-- decision -> errand: drop, replaced by DecisionService ErrandDeleted listener.
alter table if exists decision drop foreign key if exists fk_decision_errand_id;
