-- ============================================================
-- RentNest 本地开发种子数据（仅本地使用，禁止用于生产）
-- 用法：mysql -u root -p rentnest < seed.sql
-- 三账号密码均为 123456（BCrypt）
-- ============================================================

INSERT INTO users (id, phone, password_hash, nickname, role, status, real_name)
SELECT 1, '13800000001', '$2a$10$OV09Jn1XdCtyqQT1.DbnTe8C6ERaB1Y7pstAkQzXFR0uOALFmZ82a', '平台管理员', 'ADMIN', 'ACTIVE', '管理员'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE id = 1);

INSERT INTO users (id, phone, password_hash, nickname, role, status, real_name)
SELECT 2, '13800000002', '$2a$10$OV09Jn1XdCtyqQT1.DbnTe8C6ERaB1Y7pstAkQzXFR0uOALFmZ82a', '张房东', 'LANDLORD', 'ACTIVE', '张三'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE id = 2);

INSERT INTO users (id, phone, password_hash, nickname, role, status, real_name)
SELECT 3, '13800000003', '$2a$10$OV09Jn1XdCtyqQT1.DbnTe8C6ERaB1Y7pstAkQzXFR0uOALFmZ82a', '李同学', 'USER', 'ACTIVE', '李四'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE id = 3);
