-- =====================================================================
-- Återansökan form feedback (Rakel 2.0): notification preferences are
-- captured per person, so both the applicant and any co-applicant can
-- choose to be notified about the application and carry their own
-- contact details (pulled from Mina sidor at fill-in time).
--   * email / phone        — contact details used for notifications
--   * notify_by_email/_sms — the chosen notification channels (boxar)
-- Lives on errand_fa_person (one row per person on the application).
-- =====================================================================

alter table errand_fa_person
    add column email           varchar(255),
    add column phone           varchar(255),
    add column notify_by_email bit(1),
    add column notify_by_sms   bit(1);
