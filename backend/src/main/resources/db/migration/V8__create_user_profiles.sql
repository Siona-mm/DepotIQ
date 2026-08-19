CREATE TABLE user_profiles (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    display_name VARCHAR(150) NOT NULL,
    email VARCHAR(255),
    job_title VARCHAR(150),
    avatar_data TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO user_profiles (username, display_name, email, job_title)
VALUES
    ('admin', 'Admin', 'admin@depotiq.local', 'Depot Administrator'),
    ('manager', 'Manager', 'manager@depotiq.local', 'Operations Manager'),
    ('viewer', 'Viewer', 'viewer@depotiq.local', 'Supply Chain Viewer');
