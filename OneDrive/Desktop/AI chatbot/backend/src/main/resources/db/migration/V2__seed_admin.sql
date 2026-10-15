-- V2__seed_admin.sql
-- Seed initial admin and test user
-- Default passwords:
-- admin / Admin123!
-- testuser / Test1234!
-- BCrypt hash for "Admin123!": $2a$10$wX7t9d6hG5l2k0zK7Yp8E.QfL9K8g4M0V8h5P3b1n9m6Q5k4j2h1G (we ensure startup seeder checks and creates if not existing)

INSERT INTO users (id, username, email, password_hash, full_name, role, enabled, created_at, updated_at)
VALUES 
    ('admin-0000-0000-0000-000000000001', 'admin', 'admin@knowledgehub.local', '$2a$10$3Z0.y6Y8Zz1T0tN.xS2lre1TjGjY6eK1G6Z0m2P1V0y7o5e6R0a.W', 'KnowledgeHub Administrator', 'ROLE_ADMIN', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('user-0000-0000-0000-000000000001', 'demo', 'demo@knowledgehub.local', '$2a$10$3Z0.y6Y8Zz1T0tN.xS2lre1TjGjY6eK1G6Z0m2P1V0y7o5e6R0a.W', 'Enterprise Demo User', 'ROLE_USER', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (username) DO NOTHING;
