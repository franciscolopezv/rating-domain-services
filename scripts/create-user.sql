-- Create ratings user if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'ratings_user') THEN
        CREATE USER ratings_user WITH PASSWORD 'ratings_pass';
        GRANT CONNECT ON DATABASE postgres TO ratings_user;
        ALTER USER ratings_user CREATEDB;
        RAISE NOTICE 'User ratings_user created successfully';
    ELSE
        RAISE NOTICE 'User ratings_user already exists';
    END IF;
END
$$;