-- The nyansökan form asks "Vilken period är du sjukskriven utifrån aktuellt läkarintyg?" with a from and a to date.
-- Only the level was stored, so the dates the citizen filled in were dropped on submission. Återansökan asks for the
-- level alone, so both columns stay nullable.
ALTER TABLE `errand_fa_planning`
  ADD COLUMN `sick_leave_from` date DEFAULT NULL AFTER `sick_leave_level`,
  ADD COLUMN `sick_leave_to` date DEFAULT NULL AFTER `sick_leave_from`;
