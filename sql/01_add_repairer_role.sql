-- ============================================================
-- Feature 1: 维修人员角色 + 状态机重构
-- 将工单状态从4状态扩展为6状态
--   旧: 1待受理 2处理中 3已完成 4已取消
--   新: 1待受理 2已派单 3维修中 4待确认 5已完成 6已取消
-- ============================================================

-- 1. 修改 sys_user 表的 role 注释
ALTER TABLE sys_user MODIFY COLUMN role VARCHAR(20) NOT NULL DEFAULT 'STUDENT' COMMENT 'STUDENT/REPAIRER/ADMIN';

-- 2. 为 repair_order 新增维修人员相关字段
ALTER TABLE repair_order
    ADD COLUMN worker_id BIGINT DEFAULT NULL COMMENT '指派的维修人员ID' AFTER handler_id,
    ADD COLUMN assign_time DATETIME DEFAULT NULL COMMENT '派单时间' AFTER submit_time,
    ADD COLUMN worker_accept_time DATETIME DEFAULT NULL COMMENT '维修人员接单时间' AFTER accept_time,
    ADD COLUMN worker_complete_time DATETIME DEFAULT NULL COMMENT '维修人员完成时间' AFTER handle_time,
    ADD COLUMN student_confirm_time DATETIME DEFAULT NULL COMMENT '学生确认完成时间' AFTER finish_time,
    ADD INDEX idx_order_worker_status (worker_id, repair_status);

-- 3. 迁移现有工单数据的状态值
-- 旧状态2(处理中) → 新状态3(维修中)，旧状态3(已完成) → 新状态5(已完成)，旧状态4(已取消) → 新状态6(已取消)
UPDATE repair_order SET repair_status = 3 WHERE repair_status = 2;
UPDATE repair_order SET repair_status = 5 WHERE repair_status = 3;
UPDATE repair_order SET repair_status = 6 WHERE repair_status = 4;

-- 4. 插入维修人员种子账号 (密码: 123456)
INSERT INTO sys_user (username, password, real_name, phone, gender, role, status)
VALUES ('repairer01', '$2a$10$qSeBEhxriW4yykTL2IMTJOu9hgLTeSgltYkcAS707Ui9wN30QwT6e', '李师傅', '13800000002', 1, 'REPAIRER', 1);

-- 5. 更新 repair_feedback 中引用的工单状态检查保持不变（评价只依赖 repair_order_id，不依赖状态值）
