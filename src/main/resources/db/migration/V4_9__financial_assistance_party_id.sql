-- =====================================================================
-- The EB application identifies people by partyId (personId GUID), not
-- personnummer, at the API edge. Rename the stored person/child columns
-- to match. Personnummer is resolved from partyId via the citizen service
-- only when calling external systems (Lifecare, SSBTEK).
-- =====================================================================

alter table errand_fa_person
    rename column personal_number to party_id;

alter table errand_fa_child
    rename column personal_number to party_id;
