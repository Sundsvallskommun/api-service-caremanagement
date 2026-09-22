-- Logguppföljning: verksamhetens regelverk (revision 2026-09-22) requires the access log to be searchable per user,
-- not only per errand. That read selects on `actor` across every errand in the namespace and orders by `created`,
-- which the existing `(errand_id, created)` index cannot serve at all — it would be a full scan of a table that grows
-- by one row per request, forever. This is the covering index for it.
ALTER TABLE `errand_event`
  ADD KEY `idx_errand_event_actor_created` (`actor`, `created`);
