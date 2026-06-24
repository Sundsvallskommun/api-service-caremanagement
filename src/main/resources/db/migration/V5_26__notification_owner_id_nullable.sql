-- =====================================================================
-- Allow ownerless notifications.
--
-- An INBOUND message from the applicant (Mina sidor) now raises a MESSAGE
-- notification even when the errand has no handläggare assigned yet — so an
-- unassigned errand with unread messages still surfaces via the errand list
-- filter (?hasUnacknowledgedNotifications=true, with no notificationOwnerId).
-- The recipient is unknown at that point, so owner_id is left null and is
-- backfilled to the new assignee when the errand is picked up (ErrandAssigned).
--
-- owner_id therefore drops its NOT NULL constraint. The composite index
-- idx_notification_mid_ns_owner_id_acknowledged keeps working — null owners
-- simply sort together and are matched by the owner-agnostic EXISTS subquery.
-- =====================================================================

alter table notification modify column owner_id varchar(255) null;
