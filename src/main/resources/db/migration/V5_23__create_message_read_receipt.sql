-- =====================================================================
-- Per-message read receipts for the conversation thread. One row per
-- (message, reader side) records that a side — CASEWORKER (Draken) or
-- CLIENT (Mina sidor) — has read a given message. The unread count for a
-- side is the messages addressed to it (the opposite direction) that have
-- no receipt for that side yet. Read state lives here, separate from the
-- immutable message, and is never recorded in the händelselogg.
-- =====================================================================

create table message_read_receipt (
    id          varchar(255)    not null,
    message_id  varchar(255)    not null,
    reader_side varchar(16)     not null,
    read_by     varchar(64),
    read_at     datetime(6)     not null,
    primary key (id),
    constraint uk_message_read_receipt_message_side unique (message_id, reader_side)
) engine = InnoDB;

create index idx_message_read_receipt_message_id on message_read_receipt (message_id);

alter table if exists message_read_receipt
    add constraint fk_message_read_receipt_message_id
    foreign key (message_id)
    references errand_message (id)
    on delete cascade;
