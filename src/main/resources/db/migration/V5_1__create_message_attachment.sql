-- =====================================================================
-- Conversation module — file attachments on a message. Self-contained
-- within the module: an attachment is scoped to its message (which is
-- scoped to an errand). Binary content lives in message_attachment_data,
-- keyed by the attachment id, so the blob is only read when streamed.
--
-- FKs cascade along the delete origin so cleanup is atomic at the DB
-- level with no orphaned rows:
--   errand -> errand_message -> message_attachment -> message_attachment_data
-- =====================================================================

create table message_attachment (
    id          varchar(255)    not null,
    message_id  varchar(255)    not null,
    file_name   varchar(255),
    mime_type   varchar(255),
    file_size   integer,
    created     datetime(6),
    primary key (id)
) engine = InnoDB;

create table message_attachment_data (
    id                      integer         not null auto_increment,
    message_attachment_id   varchar(255)    not null,
    file                    longblob,
    primary key (id)
) engine = InnoDB;

create index idx_message_attachment_message_id on message_attachment (message_id);

alter table if exists message_attachment_data
    add constraint uq_message_attachment_data_attachment_id unique (message_attachment_id);

-- message_attachment -> errand_message: cascade so attachments disappear
-- when the message (and transitively the errand) is deleted.
alter table if exists message_attachment
    add constraint fk_message_attachment_message_id
    foreign key (message_id)
    references errand_message (id)
    on delete cascade;

-- message_attachment_data -> message_attachment: cascade so the blob row
-- is removed together with its attachment.
alter table if exists message_attachment_data
    add constraint fk_message_attachment_data_attachment_id
    foreign key (message_attachment_id)
    references message_attachment (id)
    on delete cascade;
