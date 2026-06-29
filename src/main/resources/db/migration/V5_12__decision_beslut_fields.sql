-- Beslut-tab fields on the decision audit row: the granted/recommended amount, the
-- decision message (beslutsmeddelande) shown to the applicant, the handläggare-chosen
-- decision date and the period the decision covers. All optional — only EB beslut and
-- recommendations populate them; other decisions leave them null.
alter table decision
    add column amount decimal(15,2),
    add column decision_message varchar(8192),
    add column decision_date date,
    add column period_from date,
    add column period_to date;
