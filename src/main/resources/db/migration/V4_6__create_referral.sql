-- =====================================================================
-- Referral module — a referral/consultation sent on an errand to an
-- external authority, and its response. Cleaned up via an ErrandDeleted
-- listener.
-- =====================================================================

create table referral (
    id            varchar(255)  not null,
    errand_id     varchar(255)  not null,
    authority     varchar(64),
    recipient     varchar(255),
    sent_at       date,
    due_at        date,
    response_text varchar(4096),
    status        varchar(32),
    created       datetime(6),
    modified      datetime(6),
    primary key (id)
) engine = InnoDB;

create index idx_referral_errand_id on referral (errand_id);

alter table if exists referral
    add constraint fk_referral_errand_id
    foreign key (errand_id)
    references errand (id);
