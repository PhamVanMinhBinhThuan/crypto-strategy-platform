-- F-009 correlation identifiers are opaque bounded values. The earlier constraint
-- relaxation retained the legacy varchar(26) storage width, rejecting UUID clients.
begin;

alter table experiment.job
    alter column correlation_id type varchar(128);

commit;
