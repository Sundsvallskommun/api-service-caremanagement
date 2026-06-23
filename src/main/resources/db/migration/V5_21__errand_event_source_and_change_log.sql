-- =====================================================================
-- Event log: add a `source` discriminator and relax the HTTP-only columns.
--
-- The log now has two writers:
--   HTTP  — the HandlerInterceptor, one row per errand-scoped request
--           (reads + writes), actor from the X-Sent-By header.
--   EVENT — an @ApplicationModuleListener on the published domain events
--           (errand created/status/assigned/deleted), origin-agnostic, so
--           process- and system-driven changes are captured too. These rows
--           carry no HTTP method/path/status, so those columns become nullable.
-- =====================================================================

alter table errand_event add column source varchar(16) not null default 'HTTP';

alter table errand_event modify column http_method  varchar(8)    null;
alter table errand_event modify column request_path  varchar(1024) null;
alter table errand_event modify column status_code   int           null;

create index idx_errand_event_source on errand_event (source);
