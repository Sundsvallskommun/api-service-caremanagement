-- =====================================================================
-- Financial assistance (ekonomiskt bistånd) type module.
-- One row per errand sharing the primary key with errand.id, plus owned
-- @ElementCollection value tables (errand_fa_*). The main row is removed
-- via an ErrandDeleted listener; the value tables cascade from it.
-- =====================================================================

create table errand_financial_assistance (
    errand_id                            varchar(255) not null,
    application_type                     varchar(32),
    marital_status                       varchar(32),
    period_month                         int,
    period_year                          int,
    period_choice                        varchar(32),
    norm_type                            varchar(32),
    other_benefit_description            longtext,
    livelihood_description               longtext,
    has_children_under21                 bit(1),
    children_residence_changed           bit(1),
    children_residence_change_description longtext,
    housing_form                         varchar(32),
    housing_adults_count                 int,
    housing_children_count               int,
    housing_rooms_plus_kitchen           int,
    housing_description                  longtext,
    housing_changed                      bit(1),
    housing_change_description           longtext,
    has_incomes                          bit(1),
    has_pending_benefits                 bit(1),
    has_assets                           bit(1),
    stays_in_municipality                bit(1),
    stay_description                     longtext,
    attestation                          bit(1),
    attested_at                          datetime(6),
    created                              datetime(6),
    modified                             datetime(6),
    primary key (errand_id)
) engine = InnoDB;

alter table if exists errand_financial_assistance
    add constraint fk_financial_assistance_errand_id
    foreign key (errand_id)
    references errand (id);

create table errand_fa_child (
    errand_id        varchar(255) not null,
    personal_number  varchar(255),
    first_name       varchar(255),
    last_name        varchar(255),
    school_name      varchar(255),
    residence_extent varchar(255),
    days_in_home     int
) engine = InnoDB;

create index idx_fa_child_errand_id on errand_fa_child (errand_id);

alter table if exists errand_fa_child
    add constraint fk_fa_child_errand_id
    foreign key (errand_id)
    references errand_financial_assistance (errand_id)
    on delete cascade;

create table errand_fa_cost (
    errand_id           varchar(255) not null,
    cost_type           varchar(255),
    applied_amount      decimal(12, 2),
    other_sub_type      varchar(255),
    specification       longtext,
    recipient_or_period longtext
) engine = InnoDB;

create index idx_fa_cost_errand_id on errand_fa_cost (errand_id);

alter table if exists errand_fa_cost
    add constraint fk_fa_cost_errand_id
    foreign key (errand_id)
    references errand_financial_assistance (errand_id)
    on delete cascade;

create table errand_fa_income (
    errand_id   varchar(255) not null,
    income_type varchar(255),
    amount      decimal(12, 2),
    income_date date,
    recipient   varchar(255)
) engine = InnoDB;

create index idx_fa_income_errand_id on errand_fa_income (errand_id);

alter table if exists errand_fa_income
    add constraint fk_fa_income_errand_id
    foreign key (errand_id)
    references errand_financial_assistance (errand_id)
    on delete cascade;

create table errand_fa_pending_benefit (
    errand_id      varchar(255) not null,
    benefit_name   varchar(255),
    applicant_name varchar(255)
) engine = InnoDB;

create index idx_fa_pending_benefit_errand_id on errand_fa_pending_benefit (errand_id);

alter table if exists errand_fa_pending_benefit
    add constraint fk_fa_pending_benefit_errand_id
    foreign key (errand_id)
    references errand_financial_assistance (errand_id)
    on delete cascade;

create table errand_fa_asset (
    errand_id           varchar(255) not null,
    asset_category      varchar(255),
    description         longtext,
    value               decimal(12, 2),
    property_type       varchar(255),
    purchase_year       int,
    purchase_price      decimal(12, 2),
    company_name        varchar(255),
    company_asset_sum   decimal(12, 2),
    vehicle_type        varchar(255),
    registration_number varchar(255),
    purchase_date       date
) engine = InnoDB;

create index idx_fa_asset_errand_id on errand_fa_asset (errand_id);

alter table if exists errand_fa_asset
    add constraint fk_fa_asset_errand_id
    foreign key (errand_id)
    references errand_financial_assistance (errand_id)
    on delete cascade;

create table errand_fa_person (
    errand_id                 varchar(255) not null,
    role                      varchar(255),
    personal_number           varchar(255),
    needs_interpreter         bit(1),
    interpreter_language      varchar(255),
    had_work_last12_months    bit(1),
    had_work_description      longtext,
    payment_method            varchar(255),
    clearing_number           varchar(255),
    account_number            varchar(255),
    other_payment_description longtext,
    payment_same_as_previous  bit(1)
) engine = InnoDB;

create index idx_fa_person_errand_id on errand_fa_person (errand_id);

alter table if exists errand_fa_person
    add constraint fk_fa_person_errand_id
    foreign key (errand_id)
    references errand_financial_assistance (errand_id)
    on delete cascade;

create table errand_fa_planning (
    errand_id        varchar(255) not null,
    person           varchar(255),
    planning_type    varchar(255),
    work_extent      varchar(255),
    work_description longtext,
    sick_leave_level varchar(255),
    sick_from        date,
    sick_to          date,
    sfi_study_path   varchar(255),
    sfi_course       varchar(255),
    other_description longtext
) engine = InnoDB;

create index idx_fa_planning_errand_id on errand_fa_planning (errand_id);

alter table if exists errand_fa_planning
    add constraint fk_fa_planning_errand_id
    foreign key (errand_id)
    references errand_financial_assistance (errand_id)
    on delete cascade;

create table errand_fa_planned_activity (
    errand_id   varchar(255) not null,
    person      varchar(255),
    activity    longtext,
    period_from date,
    period_to   date
) engine = InnoDB;

create index idx_fa_planned_activity_errand_id on errand_fa_planned_activity (errand_id);

alter table if exists errand_fa_planned_activity
    add constraint fk_fa_planned_activity_errand_id
    foreign key (errand_id)
    references errand_financial_assistance (errand_id)
    on delete cascade;

create table errand_fa_job_application (
    errand_id          varchar(255) not null,
    person             varchar(255),
    application_date   date,
    job_title          varchar(255),
    employer_and_place varchar(255)
) engine = InnoDB;

create index idx_fa_job_application_errand_id on errand_fa_job_application (errand_id);

alter table if exists errand_fa_job_application
    add constraint fk_fa_job_application_errand_id
    foreign key (errand_id)
    references errand_financial_assistance (errand_id)
    on delete cascade;
