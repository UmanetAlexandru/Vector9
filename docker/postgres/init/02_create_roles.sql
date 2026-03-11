DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = ''vector9_readonly'') THEN
        CREATE ROLE vector9_readonly WITH LOGIN PASSWORD ''readonly_password'';
    END IF;
END $$;

GRANT CONNECT ON DATABASE vector9_dev TO vector9_readonly;

DO $$
BEGIN
    RAISE NOTICE ''Database roles created successfully'';
END $$;