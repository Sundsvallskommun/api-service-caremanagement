-- =====================================================================
-- Conversation module — let a message reference the message it replies to
-- (quote / "svara på"). A single, optional self-reference; the UI resolves
-- the quoted message from the already-loaded thread and can link to it.
--
-- Self-FK uses ON DELETE SET NULL so removing a quoted message just clears
-- the pointer (the reply survives) rather than cascading the thread.
-- =====================================================================

alter table errand_message
    add column in_reply_to_id varchar(255) null;

create index idx_message_in_reply_to_id on errand_message (in_reply_to_id);

alter table if exists errand_message
    add constraint fk_message_in_reply_to_id
    foreign key (in_reply_to_id)
    references errand_message (id)
    on delete set null;
