-- Verksamhetens regelverk (Drakel-vyn punkt 4): en notis måste kunna markeras som "Hanterad", skilt från att bara ha
-- lästs, och ärendelistans filter går från "ärenden med olästa meddelanden" till "ärenden med ohanterade meddelanden".
-- `acknowledged` = läst, `handled` = hanterad. Att hantera implicerar läst, inte tvärtom.
ALTER TABLE `notification`
  ADD COLUMN `handled` bit(1) NOT NULL DEFAULT b'0' AFTER `acknowledged`,
  ADD KEY `idx_notification_mid_ns_owner_id_handled` (`municipality_id`,`namespace`,`owner_id`,`handled`),
  ADD KEY `idx_notification_mid_ns_errand_id_handled` (`municipality_id`,`namespace`,`errand_id`,`handled`);

-- Befintliga notiser: det som redan kvitterats räknas som hanterat vid övergången. Utan backfillen skulle hela
-- historiken dyka upp som ohanterad första gången handläggaren använder det nya filtret.
UPDATE `notification` SET `handled` = `acknowledged`;
