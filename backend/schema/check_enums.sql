SELECT n.nspname AS schema,
       t.typname AS enum_name,
       e.enumlabel AS value
FROM pg_type t
JOIN pg_enum e ON t.oid = e.enumtypid
JOIN pg_namespace n ON n.oid = t.typnamespace
WHERE n.nspname NOT IN ('pg_catalog', 'information_schema')
ORDER BY schema, enum_name, e.enumsortorder;
