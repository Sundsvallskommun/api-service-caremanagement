-- =====================================================================
-- Each calculation-draft row (person, income, expense) gains a stable
-- 0-based `position` within its errand+section. The position is assigned
-- once when the row is first persisted (caseworker add or the daily
-- prepare insert) and never changes afterwards, so the frontend can keep
-- a row in place across refreshes and simply append new rows at the end.
--
-- Existing rows are backfilled per errand ordered by creation time so
-- their current order is preserved.
-- =====================================================================

alter table errand_fa_norm_income add column position int;
alter table errand_fa_norm_expense add column position int;
alter table errand_fa_norm_person add column position int;

update errand_fa_norm_income e
    join (select id, row_number() over (partition by errand_id order by created, id) - 1 as pos
          from errand_fa_norm_income) ranked on ranked.id = e.id
set e.position = ranked.pos;

update errand_fa_norm_expense e
    join (select id, row_number() over (partition by errand_id order by created, id) - 1 as pos
          from errand_fa_norm_expense) ranked on ranked.id = e.id
set e.position = ranked.pos;

update errand_fa_norm_person e
    join (select id, row_number() over (partition by errand_id order by created, id) - 1 as pos
          from errand_fa_norm_person) ranked on ranked.id = e.id
set e.position = ranked.pos;
