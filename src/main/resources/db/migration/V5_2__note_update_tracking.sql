-- =====================================================================
-- Notes module — track edits (PATCH). Both columns are null until the
-- note is first edited.
-- =====================================================================

alter table errand_note
    add column modified_by varchar(64),
    add column modified    datetime(6);
