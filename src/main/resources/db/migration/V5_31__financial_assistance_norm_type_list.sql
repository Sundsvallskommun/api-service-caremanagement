-- =====================================================================
-- normType becomes a list. The single norm_type column on both the errand
-- (errand_financial_assistance) and the calculation-draft header
-- (errand_financial_assistance_calculation_draft) moves into its own
-- @ElementCollection value table (errand_fa_*), mirroring the other EB
-- repeating groups. Any existing single value is preserved as a
-- one-element list; the old scalar columns are then dropped.
-- =====================================================================

-- Errand norm types --------------------------------------------------------
create table errand_fa_norm_type (
    errand_id varchar(255) not null,
    norm_type varchar(32)
) engine = InnoDB;

create index idx_fa_norm_type_errand_id on errand_fa_norm_type (errand_id);

alter table if exists errand_fa_norm_type
    add constraint fk_fa_norm_type_errand_id
    foreign key (errand_id)
    references errand_financial_assistance (errand_id)
    on delete cascade;

insert into errand_fa_norm_type (errand_id, norm_type)
    select errand_id, norm_type
    from errand_financial_assistance
    where norm_type is not null;

alter table errand_financial_assistance drop column norm_type;

-- Calculation-draft norm types ---------------------------------------------
create table errand_fa_calculation_draft_norm_type (
    errand_id varchar(255) not null,
    norm_type varchar(32)
) engine = InnoDB;

create index idx_fa_calc_draft_norm_type_errand_id on errand_fa_calculation_draft_norm_type (errand_id);

alter table if exists errand_fa_calculation_draft_norm_type
    add constraint fk_fa_calc_draft_norm_type_errand_id
    foreign key (errand_id)
    references errand_financial_assistance_calculation_draft (errand_id)
    on delete cascade;

insert into errand_fa_calculation_draft_norm_type (errand_id, norm_type)
    select errand_id, norm_type
    from errand_financial_assistance_calculation_draft
    where norm_type is not null;

alter table errand_financial_assistance_calculation_draft drop column norm_type;
