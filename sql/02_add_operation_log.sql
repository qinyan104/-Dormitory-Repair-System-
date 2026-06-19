-- 操作日志表
CREATE TABLE IF NOT EXISTS `sys_operation_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `operator_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
    `operator_name` VARCHAR(50) DEFAULT NULL COMMENT '操作人姓名',
    `operation_type` VARCHAR(50) DEFAULT NULL COMMENT '操作类型',
    `description` VARCHAR(200) DEFAULT NULL COMMENT '操作描述',
    `method_name` VARCHAR(200) DEFAULT NULL COMMENT '方法名',
    `request_params` TEXT DEFAULT NULL COMMENT '请求参数',
    `ip_address` VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
    `execution_time` BIGINT DEFAULT NULL COMMENT '执行耗时(ms)',
    `status` TINYINT DEFAULT 1 COMMENT '状态 1成功 0失败',
    `error_message` VARCHAR(500) DEFAULT NULL COMMENT '异常信息',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    INDEX `idx_log_type` (`operation_type`),
    INDEX `idx_log_status` (`status`),
    INDEX `idx_log_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统操作日志';
