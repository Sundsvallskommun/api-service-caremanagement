
    create table decision (
        id varchar(255) not null,
        errand_id varchar(255) not null,
        decision_type varchar(255),
        value varchar(255),
        description varchar(4096),
        created_by varchar(255),
        created datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create index idx_decision_errand_id on decision (errand_id);
    create index idx_decision_decision_type on decision (decision_type);
    create index idx_decision_errand_id_created on decision (errand_id, created);

    alter table if exists decision
        add constraint fk_decision_errand_id
        foreign key (errand_id)
        references errand (id);
