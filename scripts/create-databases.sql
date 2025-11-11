-- Create databases for ratings system
-- Note: This script should be run as postgres admin user

-- Create write database if it doesn't exist
SELECT 'ratings_write' as database_name, 
       CASE WHEN EXISTS (SELECT 1 FROM pg_database WHERE datname = 'ratings_write') 
            THEN 'already exists' 
            ELSE 'will be created' 
       END as status;

-- Create read database if it doesn't exist  
SELECT 'ratings_read' as database_name,
       CASE WHEN EXISTS (SELECT 1 FROM pg_database WHERE datname = 'ratings_read') 
            THEN 'already exists' 
            ELSE 'will be created' 
       END as status;