-- =====================================================================
-- The consolidated client-attachment PDF (klientbilagor.pdf) consolidates
-- the client's conversation attachments, so it belongs to the CONVERSATION
-- origin rather than GENERATED — a ?origin=CONVERSATION listing then returns
-- the client's conversation files together with their consolidation.
--
-- Re-tag any rows already created before this change. The upsert that keeps
-- the PDF current now looks it up by (errand, klientbilagor.pdf, CONVERSATION),
-- so without this re-tag an existing GENERATED row would be missed and a
-- duplicate created. sammanstallning.pdf (the application combine) stays
-- GENERATED.
-- =====================================================================

update attachment
    set origin = 'CONVERSATION'
    where file_name = 'klientbilagor.pdf' and origin = 'GENERATED';
