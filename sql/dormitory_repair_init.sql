SET NAMES utf8mb4;
CREATE DATABASE IF NOT EXISTS dormitory_repair DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE dormitory_repair;

DROP TABLE IF EXISTS repair_feedback;
DROP TABLE IF EXISTS repair_order;
DROP TABLE IF EXISTS repair_category;
DROP TABLE IF EXISTS notice;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL UNIQUE,
  password VARCHAR(100) NOT NULL,
  real_name VARCHAR(50) NOT NULL,
  student_no VARCHAR(30) DEFAULT NULL,
  phone VARCHAR(20) DEFAULT NULL,
  gender TINYINT DEFAULT 0 COMMENT '0未知 1男 2女',
  dormitory_building VARCHAR(50) DEFAULT NULL,
  room_no VARCHAR(20) DEFAULT NULL,
  avatar VARCHAR(255) DEFAULT NULL,
  role VARCHAR(20) NOT NULL DEFAULT 'STUDENT' COMMENT 'STUDENT/REPAIRER/ADMIN',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
  skill_type VARCHAR(255) DEFAULT NULL COMMENT '技能类型（维修人员），逗号分隔',
  service_area VARCHAR(255) DEFAULT NULL COMMENT '负责区域（维修人员），逗号分隔',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_user_role_status (role, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE repair_category (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  category_name VARCHAR(50) NOT NULL UNIQUE,
  description VARCHAR(255) DEFAULT NULL,
  sort_num INT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_category_status_sort (status, sort_num)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报修分类表';

CREATE TABLE repair_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(50) NOT NULL UNIQUE,
  user_id BIGINT NOT NULL,
  category_id BIGINT NOT NULL,
  handler_id BIGINT DEFAULT NULL,
  worker_id BIGINT DEFAULT NULL,
  title VARCHAR(100) NOT NULL,
  content TEXT NOT NULL,
  image_url VARCHAR(255) DEFAULT NULL,
  urgency VARCHAR(20) DEFAULT NULL COMMENT '紧急程度：一般/紧急/非常紧急',
  repair_status TINYINT NOT NULL DEFAULT 1 COMMENT '1待受理 2已派单 3维修中 4待确认 5已完成 6已取消',
  submit_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  assign_time DATETIME DEFAULT NULL,
  accept_time DATETIME DEFAULT NULL,
  worker_accept_time DATETIME DEFAULT NULL,
  handle_time DATETIME DEFAULT NULL,
  worker_complete_time DATETIME DEFAULT NULL,
  finish_time DATETIME DEFAULT NULL,
  student_confirm_time DATETIME DEFAULT NULL,
  cancel_time DATETIME DEFAULT NULL,
  remark VARCHAR(500) DEFAULT NULL,
  priority_score INT DEFAULT 5 COMMENT 'AI优先级分数 1-10',
  impact_scope INT DEFAULT 3 COMMENT 'AI影响范围 1-10',
  risk_score INT DEFAULT 0 COMMENT 'AI验收风险分 0-100',
  needs_review TINYINT DEFAULT 0 COMMENT '是否需要管理员复查',
  review_type VARCHAR(32) DEFAULT NULL COMMENT '验收方式: AUTO_ACCEPT/STUDENT_CONFIRM/ADMIN_REVIEW',
  version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_order_user_status_time (user_id, repair_status, submit_time),
  INDEX idx_order_category_status_time (category_id, repair_status, submit_time),
  INDEX idx_order_status_time (repair_status, submit_time),
  INDEX idx_order_worker_status (worker_id, repair_status),
  CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
  CONSTRAINT fk_order_category FOREIGN KEY (category_id) REFERENCES repair_category(id),
  CONSTRAINT fk_order_handler FOREIGN KEY (handler_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报修单表';

CREATE TABLE repair_feedback (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  repair_order_id BIGINT NOT NULL UNIQUE,
  user_id BIGINT NOT NULL,
  score TINYINT NOT NULL COMMENT '1-5分',
  content VARCHAR(500) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_feedback_user_time (user_id, create_time),
  CONSTRAINT fk_feedback_order FOREIGN KEY (repair_order_id) REFERENCES repair_order(id),
  CONSTRAINT fk_feedback_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='维修评价表';

CREATE TABLE notice (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(100) NOT NULL,
  content TEXT NOT NULL,
  publisher VARCHAR(50) NOT NULL,
  type VARCHAR(20) DEFAULT 'GENERAL',
  is_top TINYINT NOT NULL DEFAULT 0 COMMENT '1置顶 0不置顶',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1发布 0草稿',
  publish_time DATETIME DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_notice_status_top_time (status, is_top, publish_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告表';

CREATE TABLE IF NOT EXISTS sys_operation_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  operator_id BIGINT DEFAULT NULL COMMENT '操作人ID',
  operator_name VARCHAR(50) DEFAULT NULL COMMENT '操作人姓名',
  operation_type VARCHAR(50) DEFAULT NULL COMMENT '操作类型',
  description VARCHAR(200) DEFAULT NULL COMMENT '操作描述',
  method_name VARCHAR(200) DEFAULT NULL COMMENT '方法名',
  request_params TEXT DEFAULT NULL COMMENT '请求参数',
  ip_address VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
  execution_time BIGINT DEFAULT NULL COMMENT '执行耗时(ms)',
  status TINYINT DEFAULT 1 COMMENT '1成功 0失败',
  error_message VARCHAR(500) DEFAULT NULL COMMENT '异常信息',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (id),
  INDEX idx_log_type (operation_type),
  INDEX idx_log_status (status),
  INDEX idx_log_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统操作日志';

INSERT INTO sys_user (username, password, real_name, student_no, phone, gender, dormitory_building, room_no, role, status)
VALUES
-- Seeded account plaintext password: `123456`
('admin', '$2a$10$qSeBEhxriW4yykTL2IMTJOu9hgLTeSgltYkcAS707Ui9wN30QwT6e', '系统管理员', NULL, '13800000000', 0, NULL, NULL, 'ADMIN', 1),
('student01', '$2a$10$qSeBEhxriW4yykTL2IMTJOu9hgLTeSgltYkcAS707Ui9wN30QwT6e', '张三', '20260001', '13900000001', 1, '1号楼', '301', 'STUDENT', 1),
('repairer01', '$2a$10$qSeBEhxriW4yykTL2IMTJOu9hgLTeSgltYkcAS707Ui9wN30QwT6e', '李师傅', NULL, '13800000002', 1, NULL, NULL, 'REPAIRER', 1);

INSERT INTO repair_category (category_name, description, sort_num, status)
VALUES
('水电维修', '宿舍水龙头、灯具、电路等问题', 1, 1),
('家具维修', '桌椅、床铺、柜门等设施问题', 2, 1),
('网络维修', '宿舍网络和网线问题', 3, 1),
('门锁维修', '门锁、钥匙相关问题', 4, 1);

INSERT INTO notice (title, content, publisher, type, is_top, status, publish_time)
VALUES
('宿舍报修系统上线通知', '宿舍报修系统已正式上线，欢迎同学们使用。', '系统管理员', 'GENERAL', 1, 1, NOW()),
('报修时间说明', '请在工作时间提交紧急维修申请。', '系统管理员', 'GENERAL', 0, 1, NOW());
