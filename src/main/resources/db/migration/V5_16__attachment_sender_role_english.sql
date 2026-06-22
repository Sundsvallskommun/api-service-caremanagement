-- Normalise the attachment / message sender_role value to English: HANDLAGGARE -> CASEWORKER
-- (CLIENT is unchanged). Flips the rows V5_6 backfilled and any later writes before this deploy.
update attachment set sender_role = 'CASEWORKER' where sender_role = 'HANDLAGGARE';
update message_attachment set sender_role = 'CASEWORKER' where sender_role = 'HANDLAGGARE';
