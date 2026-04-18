-- ============================================================
-- 研究生考勤及实验室预约系统 - MySQL 建表脚本
-- 数据库：lamp，字符集：utf8mb4
-- 与后端 JPA 实体对应（列名使用下划线命名，与 Spring 默认命名策略一致）
-- ============================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS lamp
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE lamp;

-- ------------------------------------------------------------
-- 1. 用户表 sys_user
-- 角色：student-研究生, teacher-教师, admin-系统管理员
-- 状态：1-正常, 0-禁用
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_user (
  id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  username     VARCHAR(64)  NOT NULL COMMENT '用户名/学号/工号',
  password     VARCHAR(64)  NOT NULL COMMENT '密码（加密）',
  name         VARCHAR(32)  NOT NULL COMMENT '姓名',
  phone        VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
  email        VARCHAR(64)  DEFAULT NULL COMMENT '邮箱',
  role         VARCHAR(20)  NOT NULL COMMENT '角色',
  status       INT          NOT NULL DEFAULT 1 COMMENT '状态 1正常 0禁用',
  create_time  DATETIME     DEFAULT NULL COMMENT '创建时间',
  update_time  DATETIME     DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username),
  KEY idx_role (role),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ------------------------------------------------------------
-- 2. 考勤记录表 attendance_record
-- 每人每天一条：签到/签退时间；状态：正常/迟到/早退/缺勤
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS attendance_record (
  id            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id       BIGINT      NOT NULL COMMENT '用户ID',
  date          DATE        NOT NULL COMMENT '考勤日期',
  check_in_time DATETIME    DEFAULT NULL COMMENT '签到时间',
  check_out_time DATETIME   DEFAULT NULL COMMENT '签退时间',
  status        VARCHAR(16) DEFAULT '正常' COMMENT '状态：正常/迟到/早退/缺勤',
  create_time   DATETIME    DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_date (user_id, date),
  KEY idx_date (date),
  KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='考勤记录表';

-- ------------------------------------------------------------
-- 3. 请假申请表 leave_apply
-- 类型：personal-事假, sick-病假, other-其他
-- 状态：待审批/已通过/已驳回
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS leave_apply (
  id             BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id        BIGINT      NOT NULL COMMENT '申请人ID',
  type           VARCHAR(20)  NOT NULL COMMENT '请假类型：personal/sick/other',
  start_time     DATETIME    NOT NULL COMMENT '开始时间',
  end_time       DATETIME    NOT NULL COMMENT '结束时间',
  course_id      BIGINT      DEFAULT NULL COMMENT '课程ID',
  course_date    DATE        DEFAULT NULL COMMENT '请假对应上课日期',
  reason         TEXT        DEFAULT NULL COMMENT '请假事由',
  status         VARCHAR(16) NOT NULL DEFAULT '待审批' COMMENT '状态：待审批/已通过/已驳回',
  approve_remark TEXT        DEFAULT NULL COMMENT '审批备注',
  create_time    DATETIME    DEFAULT NULL COMMENT '创建时间',
  update_time    DATETIME    DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_user_id (user_id),
  KEY idx_course_date (course_id, course_date),
  KEY idx_status (status),
  KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='请假申请表';

-- ------------------------------------------------------------
-- 4. 实验室表 lab
-- 状态：available-可预约, maintenance-维护中
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS lab (
  id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  name         VARCHAR(64) NOT NULL COMMENT '实验室名称',
  description  TEXT        DEFAULT NULL COMMENT '描述',
  location     VARCHAR(128) DEFAULT NULL COMMENT '位置',
  capacity     INT         DEFAULT NULL COMMENT '容量（人）',
  equipment_info VARCHAR(255) DEFAULT NULL COMMENT '设备信息',
  status       VARCHAR(20) NOT NULL DEFAULT 'available' COMMENT '状态：available/maintenance',
  PRIMARY KEY (id),
  KEY idx_status (status),
  KEY idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='实验室表';

-- ------------------------------------------------------------
-- 5. 实验室预约表 lab_booking
-- 时段示例：08:00-10:00, 10:00-12:00, 14:00-16:00, 16:00-18:00
-- 状态：pending-待审批, approved-已通过, checked_in-已签到, completed-已完成,
--       no_show-已爽约, rejected-已拒绝, cancelled-已取消
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS lab_booking (
  id              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  lab_id           BIGINT      NOT NULL COMMENT '实验室ID',
  user_id          BIGINT      NOT NULL COMMENT '预约人ID',
  date             DATE        NOT NULL COMMENT '预约日期',
  slot             VARCHAR(32) NOT NULL COMMENT '时段，如 08:00-10:00',
  purpose          TEXT        DEFAULT NULL COMMENT '用途/人数说明',
  status           VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态：pending/approved/checked_in/completed/no_show/rejected/cancelled',
  approve_remark   TEXT        DEFAULT NULL COMMENT '审批备注',
  check_in_time    DATETIME    DEFAULT NULL COMMENT '实验室签到时间',
  check_out_time   DATETIME    DEFAULT NULL COMMENT '实验室签退时间',
  create_time      DATETIME    DEFAULT NULL COMMENT '创建时间',
  update_time      DATETIME    DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_lab_date_slot (lab_id, date, slot),
  KEY idx_lab_date_status (lab_id, date, status),
  KEY idx_user_id (user_id),
  KEY idx_status_date (status, date),
  KEY idx_status (status),
  KEY idx_date (date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='实验室预约表';

-- ------------------------------------------------------------
-- 6. 课程表 course
-- 采用固定周课表：学期 + 第1周开始日期 + 周次范围 + 星期 + 起止时间
-- status：active-启用, inactive-停用
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS course (
  id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  course_code     VARCHAR(32)  NOT NULL COMMENT '课程编号',
  course_name     VARCHAR(64)  NOT NULL COMMENT '课程名称',
  teacher_id      BIGINT       NOT NULL COMMENT '授课教师ID',
  semester        VARCHAR(32)  NOT NULL COMMENT '学期',
  term_start_date DATE         NOT NULL COMMENT '第1周周一日期',
  weeks           VARCHAR(32)  NOT NULL COMMENT '上课周次，如1-16',
  weekday         INT          NOT NULL COMMENT '星期几，1-7',
  start_time      TIME         NOT NULL COMMENT '开始时间',
  end_time        TIME         NOT NULL COMMENT '结束时间',
  location        VARCHAR(64)  DEFAULT NULL COMMENT '上课地点',
  remark          TEXT         DEFAULT NULL COMMENT '备注',
  status          VARCHAR(20)  NOT NULL DEFAULT 'active' COMMENT '状态：active/inactive',
  create_time     DATETIME     DEFAULT NULL COMMENT '创建时间',
  update_time     DATETIME     DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_course_code (course_code),
  KEY idx_teacher_semester (teacher_id, semester),
  KEY idx_weekday_status (weekday, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程表';

-- ------------------------------------------------------------
-- 7. 选课关系表 course_student
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS course_student (
  id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  course_id   BIGINT      NOT NULL COMMENT '课程ID',
  student_id  BIGINT      NOT NULL COMMENT '学生ID',
  create_time DATETIME    DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_course_student (course_id, student_id),
  KEY idx_student_id (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='选课关系表';

-- ------------------------------------------------------------
-- 8. 课程考勤表 course_attendance
-- 状态：待签到/已签到/迟到/请假/缺勤
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS course_attendance (
  id            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  course_id      BIGINT      NOT NULL COMMENT '课程ID',
  student_id     BIGINT      NOT NULL COMMENT '学生ID',
  course_date    DATE        NOT NULL COMMENT '上课日期',
  status         VARCHAR(16) NOT NULL DEFAULT '待签到' COMMENT '状态',
  check_in_time  DATETIME    DEFAULT NULL COMMENT '签到时间',
  remark         TEXT        DEFAULT NULL COMMENT '备注',
  create_time    DATETIME    DEFAULT NULL COMMENT '创建时间',
  update_time    DATETIME    DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_course_student_date (course_id, student_id, course_date),
  KEY idx_student_date (student_id, course_date),
  KEY idx_course_date (course_id, course_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程考勤表';

-- ------------------------------------------------------------
-- 9. 课程联调样例数据
-- 依赖已有用户：
-- 2-7 为学生，8-10 为教师
-- ------------------------------------------------------------
INSERT INTO course (id, course_code, course_name, teacher_id, semester, term_start_date, weeks, weekday, start_time, end_time, location, remark, status, create_time, update_time) VALUES
(1, 'CS5001', '研究方法与论文写作', 8, '2025-2026-1', '2025-09-01', '1-16', 1, '08:00:00', '10:00:00', '教学楼A201', '研究生核心课程', 'active', NOW(), NOW()),
(2, 'CS5002', '高级数据库系统', 9, '2025-2026-1', '2025-09-01', '1-16', 2, '10:00:00', '12:00:00', '教学楼B305', '案例与实验结合', 'active', NOW(), NOW()),
(3, 'CS5003', '机器学习专题', 10, '2025-2026-1', '2025-09-01', '1-16', 3, '14:00:00', '16:00:00', '实验楼C402', '含课程实践', 'active', NOW(), NOW()),
(4, 'CS5004', '分布式系统设计', 8, '2025-2026-1', '2025-09-01', '1-16', 4, '16:00:00', '18:00:00', '教学楼A305', '面向软件方向学生', 'active', NOW(), NOW())
ON DUPLICATE KEY UPDATE update_time = VALUES(update_time);

INSERT INTO course_student (course_id, student_id, create_time) VALUES
(1, 2, NOW()), (1, 3, NOW()), (1, 4, NOW()),
(2, 2, NOW()), (2, 5, NOW()), (2, 6, NOW()),
(3, 2, NOW()), (3, 4, NOW()), (3, 7, NOW()),
(4, 3, NOW()), (4, 5, NOW()), (4, 6, NOW())
ON DUPLICATE KEY UPDATE create_time = VALUES(create_time);

INSERT INTO course_attendance (course_id, student_id, course_date, status, check_in_time, remark, create_time, update_time) VALUES
(1, 2, '2025-09-08', '已签到', '2025-09-08 07:58:00', NULL, NOW(), NOW()),
(1, 3, '2025-09-08', '迟到', '2025-09-08 08:12:00', NULL, NOW(), NOW()),
(2, 2, '2025-09-09', '请假', NULL, '已批准课程请假', NOW(), NOW()),
(3, 4, '2025-09-10', '已签到', '2025-09-10 13:55:00', NULL, NOW(), NOW())
ON DUPLICATE KEY UPDATE status = VALUES(status), check_in_time = VALUES(check_in_time), remark = VALUES(remark), update_time = VALUES(update_time);

-- ------------------------------------------------------------
-- 可选：初始化管理员账号（密码需与后端加密方式一致，此处仅为示例）
-- 建议通过应用启动时 CommandLineRunner 创建，此处仅作参考
-- 若使用 BCrypt，密码 admin123 的密文示例（需由应用生成后替换）：
-- INSERT INTO sys_user (username, password, name, role, status, create_time, update_time)
-- VALUES ('admin', '$2a$10$...', '系统管理员', 'admin', 1, NOW(), NOW());
-- ------------------------------------------------------------
