-- Grant permissions on databases to ratings_user
-- Note: This script should be run as postgres admin user

GRANT ALL PRIVILEGES ON DATABASE ratings_write TO ratings_user;
GRANT ALL PRIVILEGES ON DATABASE ratings_read TO ratings_user;

-- Show confirmation
SELECT 'Permissions granted to ratings_user on both databases' as status;