
    create table notification (
        id varchar(255) not null,
        errand_id varchar(255) not null,
        municipality_id varchar(8) not null,
        namespace varchar(32) not null,
        owner_id varchar(255) not null,
        created_by varchar(255),
        type varchar(32) not null,
        sub_type varchar(32),
        description varchar(512) not null,
        content varchar(2048),
        acknowledged bit(1) not null default b'0',
        expires datetime(6) not null,
        created datetime(6),
        modified datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create index idx_notification_errand_id on notification (errand_id);
    create index idx_notification_mid_ns_owner_id_acknowledged on notification (municipality_id, namespace, owner_id, acknowledged);
    create index idx_notification_mid_ns_errand_id_acknowledged on notification (municipality_id, namespace, errand_id, acknowledged);
    create index idx_notification_expires on notification (expires);

    alter table if exists notification
        add constraint fk_notification_errand_id
        foreign key (errand_id)
        references errand (id)
        on delete cascade;
