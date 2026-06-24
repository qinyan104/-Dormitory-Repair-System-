-- 为已有数据库补充 skill_type 和 service_area 字段
-- 适用于已通过 dormitory_repair_init.sql 初始化但缺少这两个字段的情况

ALTER TABLE sys_user
    ADD COLUMN IF NOT EXISTS skill_type VARCHAR(255) DEFAULT NULL COMMENT '技能类型（维修人员），逗号分隔' AFTER `status`,
    ADD COLUMN IF NOT EXISTS service_area VARCHAR(255) DEFAULT NULL COMMENT '负责区域（维修人员），逗号分隔' AFTER `skill_type`;
