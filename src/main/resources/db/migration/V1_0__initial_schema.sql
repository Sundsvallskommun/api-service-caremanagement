
    create table attachment (
        attachment_data_id integer not null,
        file_size integer,
        created datetime(6),
        modified datetime(6),
        errand_id varchar(255) not null,
        file_name varchar(255),
        id varchar(255) not null,
        mime_type varchar(255),
        municipality_id varchar(8),
        namespace varchar(32),
        primary key (id)
    ) engine=InnoDB;

    create table attachment_data (
        id integer not null auto_increment,
        file longblob,
        primary key (id)
    ) engine=InnoDB;

    create table lookup (
        created datetime(6),
        id bigint not null auto_increment,
        modified datetime(6),
        kind varchar(32) not null,
        name varchar(255) not null,
        display_name varchar(255),
        municipality_id varchar(8) not null,
        namespace varchar(32) not null,
        primary key (id)
    ) engine=InnoDB;

    create table errand (
        created datetime(6),
        modified datetime(6),
        touched datetime(6),
        assigned_user_id varchar(255),
        category varchar(255),
        contact_reason_id bigint,
        contact_reason_description varchar(4096),
        id varchar(255) not null,
        municipality_id varchar(8) not null,
        namespace varchar(32) not null,
        priority varchar(255),
        reporter_user_id varchar(255),
        status varchar(64),
        title varchar(255),
        type varchar(128),
        description longtext,
        primary key (id)
    ) engine=InnoDB;

    create table external_tag (
        errand_id varchar(255) not null,
        `key` varchar(255),
        `value` varchar(255)
    ) engine=InnoDB;

    create table parameter (
        id varchar(255) not null,
        errand_id varchar(255) not null,
        display_name varchar(255),
        parameter_group varchar(255),
        parameters_key varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table parameter_values (
        parameter_id varchar(255) not null,
        value_order integer default 0 not null,
        value varchar(3000)
    ) engine=InnoDB;

    create table stakeholder (
        id varchar(255) not null,
        errand_id varchar(255) not null,
        external_id varchar(255),
        external_id_type varchar(255),
        `role` varchar(255),
        first_name varchar(255),
        last_name varchar(255),
        organization_name varchar(255),
        address varchar(255),
        care_of varchar(255),
        zip_code varchar(255),
        city varchar(255),
        country varchar(255),
        created datetime(6),
        modified datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table contact_channel (
        stakeholder_id varchar(255) not null,
        `key` varchar(255),
        `value` varchar(255)
    ) engine=InnoDB;

    create table stakeholder_parameter (
        id bigint not null auto_increment,
        stakeholder_id varchar(255) not null,
        display_name varchar(255),
        parameters_key varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table stakeholder_parameter_values (
        stakeholder_parameter_id bigint not null,
        value varchar(255)
    ) engine=InnoDB;

    create table namespace_config (
        created datetime(6),
        id bigint not null auto_increment,
        modified datetime(6),
        display_name varchar(255),
        municipality_id varchar(8) not null,
        namespace varchar(32) not null,
        short_code varchar(16),
        primary key (id)
    ) engine=InnoDB;

    create table shedlock (
        name varchar(64) not null,
        lock_until timestamp(3) not null default current_timestamp(3) on update current_timestamp(3),
        locked_at timestamp(3) not null default current_timestamp(3),
        locked_by varchar(255) not null,
        primary key (name)
    ) engine=InnoDB;

    create index idx_attachment_file_name on attachment (file_name);
    create index idx_attachment_municipality_id on attachment (municipality_id);
    create index idx_attachment_namespace on attachment (namespace);
    alter table if exists attachment add constraint uq_attachment_data_id unique (attachment_data_id);

    create index idx_lookup_kind_namespace_municipality_id on lookup (kind, namespace, municipality_id);
    alter table if exists lookup add constraint uq_lookup_kind_namespace_municipality_id_name unique (kind, namespace, municipality_id, name);

    create index idx_errand_id on errand (id);
    create index idx_errand_namespace on errand (namespace);
    create index idx_errand_municipality_id on errand (municipality_id);
    create index idx_errand_municipality_id_namespace_status on errand (municipality_id, namespace, status);
    create index idx_errand_municipality_id_namespace_category on errand (municipality_id, namespace, category);
    create index idx_errand_municipality_id_namespace_type on errand (municipality_id, namespace, type);
    create index idx_errand_municipality_id_namespace_assigned_user_id on errand (municipality_id, namespace, assigned_user_id);
    create index idx_errand_municipality_id_namespace_reporter_user_id on errand (municipality_id, namespace, reporter_user_id);
    create index idx_errand_municipality_id_namespace_status_touched on errand (municipality_id, namespace, status, touched);
    create index idx_errand_municipality_id_namespace_status_modified on errand (municipality_id, namespace, status, modified);
    create index idx_errand_municipality_id_namespace_created on errand (municipality_id, namespace, created);
    create index idx_errand_municipality_id_namespace_touched on errand (municipality_id, namespace, touched);

    create index idx_external_tag_errand_id on external_tag (errand_id);
    create index idx_external_tag_key on external_tag (`key`);
    create index idx_external_tag_value on external_tag (`value`);
    alter table if exists external_tag add constraint uq_external_tag_errand_id_key unique (errand_id, `key`);

    create index idx_stakeholder_external_id_role_errand_id on stakeholder (external_id, `role`, errand_id);

    create index idx_contact_channel_key_value on contact_channel (`key`, `value`);
    create index idx_contact_channel_value on contact_channel (`value`);

    create index idx_namespace_config_namespace_municipality_id on namespace_config (namespace, municipality_id);
    create index idx_namespace_config_municipality_id on namespace_config (municipality_id);
    alter table if exists namespace_config add constraint uq_namespace_config_namespace_municipality_id unique (namespace, municipality_id);

    alter table if exists attachment
        add constraint fk_attachment_data_attachment
        foreign key (attachment_data_id)
        references attachment_data (id);

    alter table if exists attachment
        add constraint fk_errand_attachment_errand_id
        foreign key (errand_id)
        references errand (id);

    alter table if exists errand
        add constraint fk_errand_contact_reason_id
        foreign key (contact_reason_id)
        references lookup (id);

    alter table if exists external_tag
        add constraint fk_errand_external_tag_errand_id
        foreign key (errand_id)
        references errand (id);

    alter table if exists parameter
        add constraint fk_parameter_errand_id
        foreign key (errand_id)
        references errand (id);

    alter table if exists parameter_values
        add constraint fk_parameter_values_parameter_id
        foreign key (parameter_id)
        references parameter (id);

    alter table if exists stakeholder
        add constraint fk_stakeholder_errand_id
        foreign key (errand_id)
        references errand (id);

    alter table if exists contact_channel
        add constraint fk_stakeholder_contact_channel_stakeholder_id
        foreign key (stakeholder_id)
        references stakeholder (id);

    alter table if exists stakeholder_parameter
        add constraint fk_stakeholder_parameter_stakeholder_id
        foreign key (stakeholder_id)
        references stakeholder (id);

    alter table if exists stakeholder_parameter_values
        add constraint fk_stakeholder_parameter_values_stakeholder_parameter_id
        foreign key (stakeholder_parameter_id)
        references stakeholder_parameter (id);
