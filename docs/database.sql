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
-- 角色：student-研究生, teacher-教师, labAdmin-实验室管理员, admin-系统管理员
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
  reason         TEXT        DEFAULT NULL COMMENT '请假事由',
  status         VARCHAR(16) NOT NULL DEFAULT '待审批' COMMENT '状态：待审批/已通过/已驳回',
  approve_remark TEXT        DEFAULT NULL COMMENT '审批备注',
  create_time    DATETIME    DEFAULT NULL COMMENT '创建时间',
  update_time    DATETIME    DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_user_id (user_id),
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
  capacity     INT         DEFAULT NULL COMMENT '容量（人）',
  status       VARCHAR(20) NOT NULL DEFAULT 'available' COMMENT '状态：available/maintenance',
  open_time    VARCHAR(64) DEFAULT NULL COMMENT '开放时间描述',
  create_time  DATETIME    DEFAULT NULL COMMENT '创建时间',
  update_time  DATETIME    DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_status (status),
  KEY idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='实验室表';

-- ------------------------------------------------------------
-- 5. 实验室预约表 lab_booking
-- 时段示例：08:00-10:00, 10:00-12:00, 14:00-16:00, 16:00-18:00
-- 状态：pending-待审批, approved-已通过, rejected-已拒绝, used-已使用, cancelled-已取消
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS lab_booking (
  id              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  lab_id           BIGINT      NOT NULL COMMENT '实验室ID',
  user_id          BIGINT      NOT NULL COMMENT '预约人ID',
  date             DATE        NOT NULL COMMENT '预约日期',
  slot             VARCHAR(32) NOT NULL COMMENT '时段，如 08:00-10:00',
  purpose          TEXT        DEFAULT NULL COMMENT '用途/人数说明',
  status           VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态：pending/approved/rejected/used/cancelled',
  approve_remark   TEXT        DEFAULT NULL COMMENT '审批备注',
  create_time      DATETIME    DEFAULT NULL COMMENT '创建时间',
  update_time      DATETIME    DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_lab_date_slot (lab_id, date, slot),
  KEY idx_user_id (user_id),
  KEY idx_status (status),
  KEY idx_date (date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='实验室预约表';

-- ------------------------------------------------------------
-- 可选：初始化管理员账号（密码需与后端加密方式一致，此处仅为示例）
-- 建议通过应用启动时 CommandLineRunner 创建，此处仅作参考
-- 若使用 BCrypt，密码 admin123 的密文示例（需由应用生成后替换）：
-- INSERT INTO sys_user (username, password, name, role, status, create_time, update_time)
-- VALUES ('admin', '$2a$10$...', '系统管理员', 'admin', 1, NOW(), NOW());
-- ------------------------------------------------------------
