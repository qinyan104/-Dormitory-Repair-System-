SET NAMES utf8mb4;
CREATE DATABASE IF NOT EXISTS dormitory_repair DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE dormitory_repair;

DROP TABLE IF EXISTS repair_feedback;
DROP TABLE IF EXISTS notification;
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

CREATE TABLE notification (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL COMMENT '接收用户ID',
  type VARCHAR(50) DEFAULT NULL COMMENT '通知类型',
  title VARCHAR(100) DEFAULT NULL COMMENT '通知标题',
  content VARCHAR(500) DEFAULT NULL COMMENT '通知内容',
  order_id BIGINT DEFAULT NULL COMMENT '关联工单ID',
  is_read TINYINT DEFAULT 0 COMMENT '是否已读 0未读 1已读',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX idx_user_id (user_id),
  INDEX idx_is_read (is_read),
  INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知表';

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

UPDATE sys_user
SET skill_type = '水电维修,家具维修,门锁维修',
    service_area = '1号楼,2号楼'
WHERE username = 'repairer01';

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

INSERT INTO repair_order (
  order_no, user_id, category_id, handler_id, worker_id, title, content,
  urgency, repair_status, submit_time, assign_time, accept_time,
  worker_accept_time, handle_time, worker_complete_time, finish_time,
  student_confirm_time, remark, priority_score, impact_scope, risk_score,
  needs_review, review_type, version, create_time, update_time
)
VALUES
(
  'DEMO-20260624-001',
  (SELECT id FROM sys_user WHERE username = 'student01'),
  (SELECT id FROM repair_category WHERE category_name = '水电维修'),
  NULL,
  NULL,
  '宿舍水龙头持续漏水',
  '卫生间水龙头关闭后仍持续滴水，地面容易积水，请尽快安排检查。',
  '紧急',
  1,
  NOW() - INTERVAL 2 HOUR,
  NULL,
  NULL,
  NULL,
  NULL,
  NULL,
  NULL,
  NULL,
  NULL,
  8,
  6,
  0,
  0,
  'STUDENT_CONFIRM',
  0,
  NOW() - INTERVAL 2 HOUR,
  NOW() - INTERVAL 2 HOUR
),
(
  'DEMO-20260624-002',
  (SELECT id FROM sys_user WHERE username = 'student01'),
  (SELECT id FROM repair_category WHERE category_name = '网络维修'),
  (SELECT id FROM sys_user WHERE username = 'admin'),
  (SELECT id FROM sys_user WHERE username = 'repairer01'),
  '寝室网络频繁断开',
  '晚上上课和提交作业时网络多次断开，重启路由后只能短暂恢复。',
  '一般',
  2,
  NOW() - INTERVAL 1 DAY,
  NOW() - INTERVAL 20 HOUR,
  NOW() - INTERVAL 22 HOUR,
  NULL,
  NULL,
  NULL,
  NULL,
  NULL,
  '已受理并派单，请维修人员按预约时间处理。',
  5,
  4,
  0,
  0,
  'STUDENT_CONFIRM',
  0,
  NOW() - INTERVAL 1 DAY,
  NOW() - INTERVAL 20 HOUR
),
(
  'DEMO-20260624-003',
  (SELECT id FROM sys_user WHERE username = 'student01'),
  (SELECT id FROM repair_category WHERE category_name = '家具维修'),
  (SELECT id FROM sys_user WHERE username = 'admin'),
  (SELECT id FROM sys_user WHERE username = 'repairer01'),
  '书桌抽屉滑轨损坏',
  '书桌右侧抽屉无法正常拉出，滑轨变形并有松动。',
  '一般',
  3,
  NOW() - INTERVAL 2 DAY,
  NOW() - INTERVAL 42 HOUR,
  NOW() - INTERVAL 44 HOUR,
  NOW() - INTERVAL 40 HOUR,
  NOW() - INTERVAL 40 HOUR,
  NULL,
  NULL,
  NULL,
  '维修人员已接单，正在处理配件更换。',
  4,
  3,
  0,
  0,
  'STUDENT_CONFIRM',
  0,
  NOW() - INTERVAL 2 DAY,
  NOW() - INTERVAL 40 HOUR
),
(
  'DEMO-20260624-004',
  (SELECT id FROM sys_user WHERE username = 'student01'),
  (SELECT id FROM repair_category WHERE category_name = '门锁维修'),
  (SELECT id FROM sys_user WHERE username = 'admin'),
  (SELECT id FROM sys_user WHERE username = 'repairer01'),
  '宿舍门锁开合卡顿',
  '门锁转动时阻力明显，钥匙偶尔无法拔出，存在安全隐患。',
  '紧急',
  4,
  NOW() - INTERVAL 4 DAY,
  NOW() - INTERVAL 90 HOUR,
  NOW() - INTERVAL 92 HOUR,
  NOW() - INTERVAL 88 HOUR,
  NOW() - INTERVAL 88 HOUR,
  NOW() - INTERVAL 76 HOUR,
  NULL,
  NULL,
  '已完成门锁润滑和锁芯调试，请学生确认。',
  7,
  5,
  18,
  0,
  'STUDENT_CONFIRM',
  0,
  NOW() - INTERVAL 4 DAY,
  NOW() - INTERVAL 76 HOUR
),
(
  'DEMO-20260624-005',
  (SELECT id FROM sys_user WHERE username = 'student01'),
  (SELECT id FROM repair_category WHERE category_name = '水电维修'),
  (SELECT id FROM sys_user WHERE username = 'admin'),
  (SELECT id FROM sys_user WHERE username = 'repairer01'),
  '阳台照明灯不亮',
  '阳台灯管损坏，夜间晾晒衣物不方便，需要更换灯管。',
  '一般',
  5,
  NOW() - INTERVAL 7 DAY,
  NOW() - INTERVAL 6 DAY,
  NOW() - INTERVAL 6 DAY,
  NOW() - INTERVAL 5 DAY,
  NOW() - INTERVAL 5 DAY,
  NOW() - INTERVAL 4 DAY,
  NOW() - INTERVAL 3 DAY,
  NOW() - INTERVAL 3 DAY,
  '已更换灯管并测试正常。',
  3,
  2,
  5,
  0,
  'AUTO_ACCEPT',
  0,
  NOW() - INTERVAL 7 DAY,
  NOW() - INTERVAL 3 DAY
);

INSERT INTO repair_feedback (repair_order_id, user_id, score, content, create_time)
VALUES (
  (SELECT id FROM repair_order WHERE order_no = 'DEMO-20260624-005'),
  (SELECT id FROM sys_user WHERE username = 'student01'),
  5,
  '维修很及时，处理后使用正常。',
  NOW() - INTERVAL 3 DAY
);

INSERT INTO notification (user_id, type, title, content, order_id, is_read, create_time)
VALUES
(
  (SELECT id FROM sys_user WHERE username = 'admin'),
  'NEW_ORDER',
  '有新的待受理工单',
  '学生提交了水龙头漏水报修，请及时受理。',
  (SELECT id FROM repair_order WHERE order_no = 'DEMO-20260624-001'),
  0,
  NOW() - INTERVAL 2 HOUR
),
(
  (SELECT id FROM sys_user WHERE username = 'student01'),
  'ORDER_STATUS',
  '工单等待确认',
  '门锁维修已完成，请确认处理结果。',
  (SELECT id FROM repair_order WHERE order_no = 'DEMO-20260624-004'),
  0,
  NOW() - INTERVAL 76 HOUR
),
(
  (SELECT id FROM sys_user WHERE username = 'repairer01'),
  'ORDER_ASSIGNED',
  '你有新的维修任务',
  '网络维修工单已派给你，请及时接单。',
  (SELECT id FROM repair_order WHERE order_no = 'DEMO-20260624-002'),
  0,
  NOW() - INTERVAL 20 HOUR
);
