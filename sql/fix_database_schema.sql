-- ============================================
-- 数据库修复 SQL（MySQL 8.0 兼容）
-- 用途：为现有数据库补充所有缺失的字段
--
-- 执行方式：
--   1. 在数据库管理工具中直接执行本文件内容
--   2. 或：mysql -u root -p dormitory_repair < sql/fix_database_schema.sql
--   3. 或：docker exec -i repair-db mysql -u root -proot dormitory_repair < sql/fix_database_schema.sql
--
-- 注意：本 SQL 安全可重复执行，已存在的字段会被跳过
-- ============================================

USE dormitory_repair;

-- ========== 清理可能存在的旧存储过程 ==========
DROP PROCEDURE IF EXISTS safe_add_column;

-- ========== 创建安全添加字段的存储过程 ==========
DELIMITER //

CREATE PROCEDURE safe_add_column(
    IN p_table_name VARCHAR(64),
    IN p_column_name VARCHAR(64),
    IN p_column_definition TEXT
)
BEGIN
    DECLARE column_exists INT DEFAULT 0;
    
    SELECT COUNT(*) INTO column_exists
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = p_table_name
    AND COLUMN_NAME = p_column_name;
    
    IF column_exists = 0 THEN
        SET @sql = CONCAT('ALTER TABLE `', p_table_name, '` ADD COLUMN `', p_column_name, '` ', p_column_definition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        SELECT CONCAT('✅ 已添加字段: ', p_table_name, '.', p_column_name) AS status;
    ELSE
        SELECT CONCAT('⏭️  字段已存在，跳过: ', p_table_name, '.', p_column_name) AS status;
    END IF;
END //

DELIMITER ;

-- ========== 修复 sys_user 表 ==========

CALL safe_add_column('sys_user', 'skill_type', 
    "VARCHAR(255) DEFAULT NULL COMMENT '技能类型（维修人员），逗号分隔' AFTER `status`");

CALL safe_add_column('sys_user', 'service_area', 
    "VARCHAR(255) DEFAULT NULL COMMENT '负责区域（维修人员），逗号分隔' AFTER `skill_type`");

-- ========== 修复 repair_order 表 ==========

CALL safe_add_column('repair_order', 'urgency', 
    "VARCHAR(20) DEFAULT NULL COMMENT '紧急程度：一般/紧急/非常紧急' AFTER `image_url`");

CALL safe_add_column('repair_order', 'priority_score', 
    "INT DEFAULT 5 COMMENT 'AI优先级分数 1-10' AFTER `remark`");

CALL safe_add_column('repair_order', 'impact_scope', 
    "INT DEFAULT 3 COMMENT 'AI影响范围 1-10' AFTER `priority_score`");

CALL safe_add_column('repair_order', 'risk_score', 
    "INT DEFAULT 0 COMMENT 'AI验收风险分 0-100' AFTER `impact_scope`");

CALL safe_add_column('repair_order', 'needs_review', 
    "TINYINT DEFAULT 0 COMMENT '是否需要管理员复查' AFTER `risk_score`");

CALL safe_add_column('repair_order', 'review_type', 
    "VARCHAR(32) DEFAULT NULL COMMENT '验收方式: AUTO_ACCEPT/STUDENT_CONFIRM/ADMIN_REVIEW' AFTER `needs_review`");

-- ========== 验证修复结果 ==========
SELECT '✅ 数据库修复完成！请重启后端服务。' AS final_status;

-- 清理临时存储过程
DROP PROCEDURE IF EXISTS safe_add_column;
