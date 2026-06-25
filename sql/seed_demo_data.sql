SET NAMES utf8mb4;
USE dormitory_repair;

CREATE TABLE IF NOT EXISTS notification (
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

UPDATE sys_user
SET
  skill_type = COALESCE(NULLIF(skill_type, ''), '水电维修,家具维修,门锁维修'),
  service_area = COALESCE(NULLIF(service_area, ''), '1号楼,2号楼')
WHERE username = 'repairer01';

INSERT INTO repair_order (
  order_no, user_id, category_id, handler_id, worker_id, title, content,
  urgency, repair_status, submit_time, assign_time, accept_time,
  worker_accept_time, handle_time, worker_complete_time, finish_time,
  student_confirm_time, remark, priority_score, impact_scope, risk_score,
  needs_review, review_type, version, create_time, update_time
)
SELECT
  'DEMO-20260624-001', student.id, category.id, NULL, NULL,
  '宿舍水龙头持续漏水',
  '卫生间水龙头关闭后仍持续滴水，地面容易积水，请尽快安排检查。',
  '紧急', 1,
  NOW() - INTERVAL 2 HOUR, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
  NULL, 8, 6, 0, 0, 'STUDENT_CONFIRM', 0,
  NOW() - INTERVAL 2 HOUR, NOW() - INTERVAL 2 HOUR
FROM sys_user student
JOIN repair_category category ON category.category_name = '水电维修'
WHERE student.username = 'student01'
  AND NOT EXISTS (SELECT 1 FROM repair_order WHERE order_no = 'DEMO-20260624-001');

INSERT INTO repair_order (
  order_no, user_id, category_id, handler_id, worker_id, title, content,
  urgency, repair_status, submit_time, assign_time, accept_time,
  worker_accept_time, handle_time, worker_complete_time, finish_time,
  student_confirm_time, remark, priority_score, impact_scope, risk_score,
  needs_review, review_type, version, create_time, update_time
)
SELECT
  'DEMO-20260624-002', student.id, category.id, admin.id, worker.id,
  '寝室网络频繁断开',
  '晚上上课和提交作业时网络多次断开，重启路由后只能短暂恢复。',
  '一般', 2,
  NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 20 HOUR, NOW() - INTERVAL 22 HOUR,
  NULL, NULL, NULL, NULL, NULL,
  '已受理并派单，请维修人员按预约时间处理。', 5, 4, 0, 0, 'STUDENT_CONFIRM', 0,
  NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 20 HOUR
FROM sys_user student
JOIN sys_user admin ON admin.username = 'admin'
JOIN sys_user worker ON worker.username = 'repairer01'
JOIN repair_category category ON category.category_name = '网络维修'
WHERE student.username = 'student01'
  AND NOT EXISTS (SELECT 1 FROM repair_order WHERE order_no = 'DEMO-20260624-002');

INSERT INTO repair_order (
  order_no, user_id, category_id, handler_id, worker_id, title, content,
  urgency, repair_status, submit_time, assign_time, accept_time,
  worker_accept_time, handle_time, worker_complete_time, finish_time,
  student_confirm_time, remark, priority_score, impact_scope, risk_score,
  needs_review, review_type, version, create_time, update_time
)
SELECT
  'DEMO-20260624-003', student.id, category.id, admin.id, worker.id,
  '书桌抽屉滑轨损坏',
  '书桌右侧抽屉无法正常拉出，滑轨变形并有松动。',
  '一般', 3,
  NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 42 HOUR, NOW() - INTERVAL 44 HOUR,
  NOW() - INTERVAL 40 HOUR, NOW() - INTERVAL 40 HOUR, NULL, NULL, NULL,
  '维修人员已接单，正在处理配件更换。', 4, 3, 0, 0, 'STUDENT_CONFIRM', 0,
  NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 40 HOUR
FROM sys_user student
JOIN sys_user admin ON admin.username = 'admin'
JOIN sys_user worker ON worker.username = 'repairer01'
JOIN repair_category category ON category.category_name = '家具维修'
WHERE student.username = 'student01'
  AND NOT EXISTS (SELECT 1 FROM repair_order WHERE order_no = 'DEMO-20260624-003');

INSERT INTO repair_order (
  order_no, user_id, category_id, handler_id, worker_id, title, content,
  urgency, repair_status, submit_time, assign_time, accept_time,
  worker_accept_time, handle_time, worker_complete_time, finish_time,
  student_confirm_time, remark, priority_score, impact_scope, risk_score,
  needs_review, review_type, version, create_time, update_time
)
SELECT
  'DEMO-20260624-004', student.id, category.id, admin.id, worker.id,
  '宿舍门锁开合卡顿',
  '门锁转动时阻力明显，钥匙偶尔无法拔出，存在安全隐患。',
  '紧急', 4,
  NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 90 HOUR, NOW() - INTERVAL 92 HOUR,
  NOW() - INTERVAL 88 HOUR, NOW() - INTERVAL 88 HOUR, NOW() - INTERVAL 76 HOUR,
  NULL, NULL,
  '已完成门锁润滑和锁芯调试，请学生确认。', 7, 5, 18, 0, 'STUDENT_CONFIRM', 0,
  NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 76 HOUR
FROM sys_user student
JOIN sys_user admin ON admin.username = 'admin'
JOIN sys_user worker ON worker.username = 'repairer01'
JOIN repair_category category ON category.category_name = '门锁维修'
WHERE student.username = 'student01'
  AND NOT EXISTS (SELECT 1 FROM repair_order WHERE order_no = 'DEMO-20260624-004');

INSERT INTO repair_order (
  order_no, user_id, category_id, handler_id, worker_id, title, content,
  urgency, repair_status, submit_time, assign_time, accept_time,
  worker_accept_time, handle_time, worker_complete_time, finish_time,
  student_confirm_time, remark, priority_score, impact_scope, risk_score,
  needs_review, review_type, version, create_time, update_time
)
SELECT
  'DEMO-20260624-005', student.id, category.id, admin.id, worker.id,
  '阳台照明灯不亮',
  '阳台灯管损坏，夜间晾晒衣物不方便，需要更换灯管。',
  '一般', 5,
  NOW() - INTERVAL 7 DAY, NOW() - INTERVAL 6 DAY, NOW() - INTERVAL 6 DAY,
  NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 4 DAY,
  NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 3 DAY,
  '已更换灯管并测试正常。', 3, 2, 5, 0, 'AUTO_ACCEPT', 0,
  NOW() - INTERVAL 7 DAY, NOW() - INTERVAL 3 DAY
FROM sys_user student
JOIN sys_user admin ON admin.username = 'admin'
JOIN sys_user worker ON worker.username = 'repairer01'
JOIN repair_category category ON category.category_name = '水电维修'
WHERE student.username = 'student01'
  AND NOT EXISTS (SELECT 1 FROM repair_order WHERE order_no = 'DEMO-20260624-005');

INSERT INTO repair_feedback (repair_order_id, user_id, score, content, create_time)
SELECT repair_order.id, repair_order.user_id, 5, '维修很及时，处理后使用正常。', NOW() - INTERVAL 3 DAY
FROM repair_order
WHERE repair_order.order_no = 'DEMO-20260624-005'
  AND NOT EXISTS (
    SELECT 1 FROM repair_feedback WHERE repair_feedback.repair_order_id = repair_order.id
  );

INSERT INTO notification (user_id, type, title, content, order_id, is_read, create_time)
SELECT admin.id, 'NEW_ORDER', '有新的待受理工单', '学生提交了水龙头漏水报修，请及时受理。', repair_order.id, 0, NOW() - INTERVAL 2 HOUR
FROM sys_user admin
JOIN repair_order ON repair_order.order_no = 'DEMO-20260624-001'
WHERE admin.username = 'admin'
  AND NOT EXISTS (
    SELECT 1 FROM notification
    WHERE notification.user_id = admin.id
      AND notification.order_id = repair_order.id
      AND notification.title = '有新的待受理工单'
  );

INSERT INTO notification (user_id, type, title, content, order_id, is_read, create_time)
SELECT student.id, 'ORDER_STATUS', '工单等待确认', '门锁维修已完成，请确认处理结果。', repair_order.id, 0, NOW() - INTERVAL 76 HOUR
FROM sys_user student
JOIN repair_order ON repair_order.order_no = 'DEMO-20260624-004'
WHERE student.username = 'student01'
  AND NOT EXISTS (
    SELECT 1 FROM notification
    WHERE notification.user_id = student.id
      AND notification.order_id = repair_order.id
      AND notification.title = '工单等待确认'
  );

INSERT INTO notification (user_id, type, title, content, order_id, is_read, create_time)
SELECT worker.id, 'ORDER_ASSIGNED', '你有新的维修任务', '网络维修工单已派给你，请及时接单。', repair_order.id, 0, NOW() - INTERVAL 20 HOUR
FROM sys_user worker
JOIN repair_order ON repair_order.order_no = 'DEMO-20260624-002'
WHERE worker.username = 'repairer01'
  AND NOT EXISTS (
    SELECT 1 FROM notification
    WHERE notification.user_id = worker.id
      AND notification.order_id = repair_order.id
      AND notification.title = '你有新的维修任务'
  );
