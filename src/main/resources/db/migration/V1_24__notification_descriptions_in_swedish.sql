-- Notification descriptions are shown to caseworkers in Draken as is, and careM wrote its own in English (assignment)
-- or as raw codes ("Decision recorded: PAYMENT = AVSLAG"). New notifications are written in Swedish by
-- DecisionNotificationText and ErrandService; this rewrites the stored ones the same way. A type or value without a
-- translation keeps its raw code, as in DecisionNotificationText.
UPDATE `notification`
  SET `description` = 'Nytt ärende har tilldelats dig'
  WHERE `description` = 'New errand assigned to you';

UPDATE `notification`
  SET `description` = 'Ärendet har tilldelats dig'
  WHERE `description` = 'Errand reassigned to you';

-- 'Decision recorded: ' is 19 characters; the type runs up to ' = ', the value follows it.
UPDATE `notification`
  SET `description` = CONCAT(
    CASE SUBSTRING_INDEX(SUBSTRING(`description`, 20), ' = ', 1)
      WHEN 'PAYMENT' THEN 'Utbetalningsbeslut'
      WHEN 'RECOMMENDATION' THEN 'Rekommendation'
      WHEN 'ACTUALISATION' THEN 'Aktualisering'
      ELSE SUBSTRING_INDEX(SUBSTRING(`description`, 20), ' = ', 1)
    END,
    CASE SUBSTRING(`description`, LOCATE(' = ', `description`) + 3)
      WHEN '' THEN ''
      WHEN 'null' THEN ''
      WHEN 'BIFALL' THEN ': bifall'
      WHEN 'DELAVSLAG' THEN ': delvis bifall'
      WHEN 'AVSLAG' THEN ': avslag'
      WHEN 'APPROVED' THEN ': beviljat'
      WHEN 'REJECTED' THEN ': avslag'
      WHEN 'OK' THEN ': inga anmärkningar'
      WHEN 'REVIEW_REQUIRED' THEN ': kräver granskning'
      ELSE CONCAT(': ', SUBSTRING(`description`, LOCATE(' = ', `description`) + 3))
    END)
  WHERE `sub_type` = 'DECISION'
    AND `description` LIKE 'Decision recorded: % = %';
