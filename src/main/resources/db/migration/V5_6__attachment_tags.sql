-- =====================================================================
-- Tag attachments so a handläggare sees one unified, sorted document list
-- with two facets per file:
--   origin       APPLICATION | CONVERSATION | GENERATED | ERRAND
--   sender_role  CLIENT | HANDLAGGARE   (who the file came from)
--
-- Conversation message attachments carry their own sender_role, denormalised
-- from the parent message direction (INBOUND = client, OUTBOUND = handläggare)
-- so the consolidation + listing never has to re-join to read it.
--
-- Values are constrained in the API layer (String + @OneOf), not as a DB
-- enum/check, matching the existing direction column on errand_message.
-- =====================================================================

alter table attachment
    add column origin      varchar(32),
    add column sender_role varchar(32);

alter table message_attachment
    add column sender_role varchar(32);

-- Backfill existing rows so history is tagged consistently with new writes.
-- Errand attachments today are the citizen's application files plus the
-- create-time combined PDF; tag the combined one GENERATED, the rest
-- APPLICATION. sender_role stays NULL for these historical rows — they
-- predate the distinction and we cannot retro-attribute them safely.
update attachment
    set origin = case when file_name = 'sammanstallning.pdf' then 'GENERATED' else 'APPLICATION' end
    where origin is null;

-- Conversation attachments: derive sender_role from the parent message's direction.
update message_attachment ma
    join errand_message m on m.id = ma.message_id
    set ma.sender_role = case when m.direction = 'INBOUND' then 'CLIENT' else 'HANDLAGGARE' end
    where ma.sender_role is null;
