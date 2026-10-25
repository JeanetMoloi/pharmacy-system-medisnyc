# MediSync

## Audit log database migration

Existing databases need a one-time column change because `ddl-auto=update` does not widen columns:

```sql
ALTER TABLE audit_logs MODIFY COLUMN details TEXT;
```

Alternatively, use `VARCHAR(2000)` for the `details` column.
