CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "btree_gin";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

DO $$
BEGIN
    RAISE NOTICE ''PostgreSQL extensions created successfully'';
END $$;