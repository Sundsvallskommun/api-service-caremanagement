-- finalize now disposes of careM's normberäkning draft once the errand is decided on a normberäkning saved in Lifecare
-- (lifecare_calculation_id). The draft was a proposal, frozen since that id was set; the decided calculation is
-- Lifecare's, so keeping careM's copy only stores the household's incomes and expenses twice. This applies the same rule
-- to the errands decided before the change: a PAYMENT decision and a linked Lifecare calculation. The draft's norm types,
-- persons, incomes and expenses follow through their ON DELETE CASCADE foreign keys. A draft of an errand decided
-- without a saved calculation stays, as finalize leaves it.
DELETE draft
  FROM errand_financial_assistance_calculation_draft draft
  JOIN errand_financial_assistance fa ON fa.errand_id = draft.errand_id
 WHERE fa.lifecare_calculation_id IS NOT NULL
   AND EXISTS (SELECT 1 FROM decision d WHERE d.errand_id = draft.errand_id AND d.decision_type = 'PAYMENT');
