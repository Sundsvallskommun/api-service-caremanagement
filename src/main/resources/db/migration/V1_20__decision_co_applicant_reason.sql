-- The co-applicant's orsak on a decision. Draken's decision tab lets the caseworker pick one for each person in the
-- household, from the same reasonOptions; until now only the applicant's reached the decision (as its description),
-- and the co-applicant's was lost at finalize. 255, not description's 4096: it is a catalogue value, and the row
-- already sits close to MariaDB's 65535-byte row limit (description 4096 + decision_message 8192, utf8mb4).
ALTER TABLE `decision`
  ADD COLUMN `co_applicant_reason` VARCHAR(255) NULL AFTER `description`;
