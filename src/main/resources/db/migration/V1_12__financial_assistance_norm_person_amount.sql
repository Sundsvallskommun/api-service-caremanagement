-- Lifecares Beräkning-vy visar ett Belopp per hushållsmedlem (personens andel av normen, t.ex. 1431,00 för
-- Ensamstående). Draften hade ingen motsvarighet — bara jobbstimulansbeloppet — så Draken kunde inte visa kolumnen.
-- `amount` är ett processvärde: det skrivs av den dagliga prepare-körningen från föregående Lifecare-beräknings
-- CalculationPerson.Amount och uppdateras aldrig av handläggaren.
ALTER TABLE `errand_fa_norm_person`
  ADD COLUMN `amount` decimal(12,2) DEFAULT NULL AFTER `norm_interval`;
