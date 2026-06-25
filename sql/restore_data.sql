-- ============================================
-- 数据恢复脚本
-- 用途：恢复丢失的初始数据
-- 执行方式：在数据库管理工具中执行本文件内容
-- ============================================

USE dormitory_repair;

-- ========== 恢复用户数据 ==========
-- 密码都是 123456 的 BCrypt 哈希值
INSERT INTO sys_user (username, password, real_name, student_no, phone, gender, dormitory_building, room_no, role, status)
VALUES
('admin', '$2a$10$qSeBEhxriW4yykTL2IMTJOu9hgLTeSgltYkcAS707Ui9wN30QwT6e', '系统管理员', NULL, '13800000000', 0, NULL, NULL, 'ADMIN', 1),
('student01', '$2a$10$qSeBEhxriW4yykTL2IMTJOu9hgLTeSgltYkcAS707Ui9wN30QwT6e', '张三', '20260001', '13900000001', 1, '1号楼', '301', 'STUDENT', 1),
('repairer01', '$2a$10$qSeBEhxriW4yykTL2IMTJOu9hgLTeSgltYkcAS707Ui9wN30QwT6e', '李师傅', NULL, '13800000002', 1, NULL, NULL, 'REPAIRER', 1);

-- ========== 恢复报修分类 ==========
INSERT INTO repair_category (category_name, description, sort_num, status)
VALUES
('水电维修', '宿舍水龙头、灯具、电路等问题', 1, 1),
('家具维修', '桌椅、床铺、柜门等设施问题', 2, 1),
('网络维修', '宿舍网络和网线问题', 3, 1),
('门锁维修', '门锁、钥匙相关问题', 4, 1);

-- ========== 恢复公告 ==========
INSERT INTO notice (title, content, publisher, type, is_top, status, publish_time)
VALUES
('宿舍报修系统上线通知', '宿舍报修系统已正式上线，欢迎同学们使用。', '系统管理员', 'GENERAL', 1, 1, NOW()),
('报修时间说明', '请在工作时间提交紧急维修申请。', '系统管理员', 'GENERAL', 0, 1, NOW());

SELECT '✅ 数据恢复完成！' AS status;
