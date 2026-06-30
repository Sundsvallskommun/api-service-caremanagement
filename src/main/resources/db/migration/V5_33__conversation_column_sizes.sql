-- Align the message body column size with the application contract.
--   errand_message.body: longtext -> varchar(8192) to match CreateMessage.body's @Size(max = 8192).
-- (message_attachment_data.message_attachment_id is intentionally left at varchar(255): it is part of the
--  fk_message_attachment_data_attachment_id foreign key, so shrinking it would require also resizing the referenced
--  message_attachment.id and every other UUID id column — a fleet-wide change out of scope for this PR.)
alter table errand_message
    modify column body varchar(8192) not null;
