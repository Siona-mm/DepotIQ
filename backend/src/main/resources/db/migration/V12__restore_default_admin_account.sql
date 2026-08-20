DO $$
DECLARE
    current_admin_username VARCHAR(100);
BEGIN
    SELECT username
    INTO current_admin_username
    FROM app_users
    WHERE role = 'ADMIN'
    ORDER BY id
    LIMIT 1;

    IF current_admin_username IS NOT NULL
        AND LOWER(current_admin_username) <> 'admin' THEN
        DELETE FROM user_profiles
        WHERE LOWER(username) = 'admin'
            AND username <> current_admin_username;

        DELETE FROM user_settings
        WHERE LOWER(username) = 'admin'
            AND username <> current_admin_username;

        UPDATE user_profiles
        SET username = 'admin', updated_at = CURRENT_TIMESTAMP
        WHERE username = current_admin_username;

        UPDATE user_settings
        SET username = 'admin', updated_at = CURRENT_TIMESTAMP
        WHERE username = current_admin_username;
    END IF;

    DELETE FROM app_users
    WHERE role = 'ADMIN';
END $$;
