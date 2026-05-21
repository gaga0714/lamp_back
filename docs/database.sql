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
-- 角色：student-研究生, teacher-教师, admin-实验室管理员
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
-- 9. 八张表联调样例数据
-- 所有示例账号初始密码均为：123456
-- 1 为管理员，2-7 为学生，8-10 为教师
-- ------------------------------------------------------------
INSERT INTO sys_user (id, username, password, name, phone, email, role, status, create_time, update_time) VALUES
(1, 'admin', '$2a$10$QGx.JbaZNz5Ho5CLv5N82eMtGnKGWR/FAFcTc3fy/v25e49wMFsFy', '系统管理员', '13800000000', 'admin@lamp.edu.cn', 'admin', 1, NOW(), NOW()),
(2, '2024001', '$2a$10$QGx.JbaZNz5Ho5CLv5N82eMtGnKGWR/FAFcTc3fy/v25e49wMFsFy', '张明', '13800000001', '2024001@stu.lamp.edu.cn', 'student', 1, NOW(), NOW()),
(3, '2024002', '$2a$10$QGx.JbaZNz5Ho5CLv5N82eMtGnKGWR/FAFcTc3fy/v25e49wMFsFy', '李娜', '13800000002', '2024002@stu.lamp.edu.cn', 'student', 1, NOW(), NOW()),
(4, '2024003', '$2a$10$QGx.JbaZNz5Ho5CLv5N82eMtGnKGWR/FAFcTc3fy/v25e49wMFsFy', '王强', '13800000003', '2024003@stu.lamp.edu.cn', 'student', 1, NOW(), NOW()),
(5, '2024004', '$2a$10$QGx.JbaZNz5Ho5CLv5N82eMtGnKGWR/FAFcTc3fy/v25e49wMFsFy', '赵敏', '13800000004', '2024004@stu.lamp.edu.cn', 'student', 1, NOW(), NOW()),
(6, '2024005', '$2a$10$QGx.JbaZNz5Ho5CLv5N82eMtGnKGWR/FAFcTc3fy/v25e49wMFsFy', '陈宇', '13800000005', '2024005@stu.lamp.edu.cn', 'student', 1, NOW(), NOW()),
(7, '2024006', '$2a$10$QGx.JbaZNz5Ho5CLv5N82eMtGnKGWR/FAFcTc3fy/v25e49wMFsFy', '刘洋', '13800000006', '2024006@stu.lamp.edu.cn', 'student', 1, NOW(), NOW()),
(8, 'T202401', '$2a$10$QGx.JbaZNz5Ho5CLv5N82eMtGnKGWR/FAFcTc3fy/v25e49wMFsFy', '周教授', '13900000001', 'zhou@lamp.edu.cn', 'teacher', 1, NOW(), NOW()),
(9, 'T202402', '$2a$10$QGx.JbaZNz5Ho5CLv5N82eMtGnKGWR/FAFcTc3fy/v25e49wMFsFy', '吴教授', '13900000002', 'wu@lamp.edu.cn', 'teacher', 1, NOW(), NOW()),
(10, 'T202403', '$2a$10$QGx.JbaZNz5Ho5CLv5N82eMtGnKGWR/FAFcTc3fy/v25e49wMFsFy', '郑教授', '13900000003', 'zheng@lamp.edu.cn', 'teacher', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE password = VALUES(password), name = VALUES(name), phone = VALUES(phone), email = VALUES(email), role = VALUES(role), status = VALUES(status), update_time = VALUES(update_time);

INSERT INTO attendance_record (id, user_id, date, check_in_time, check_out_time, status, create_time) VALUES
(1, 2, DATE_SUB(CURDATE(), INTERVAL 2 DAY), DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 2 DAY), INTERVAL 8 HOUR), DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 2 DAY), INTERVAL 18 HOUR), '正常', NOW()),
(2, 3, DATE_SUB(CURDATE(), INTERVAL 2 DAY), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '08:18:00'), DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 2 DAY), INTERVAL 18 HOUR), '迟到', NOW()),
(3, 4, DATE_SUB(CURDATE(), INTERVAL 2 DAY), DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 2 DAY), INTERVAL 8 HOUR), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '16:30:00'), '早退', NOW()),
(4, 5, DATE_SUB(CURDATE(), INTERVAL 2 DAY), NULL, NULL, '缺勤', NOW()),
(5, 2, DATE_SUB(CURDATE(), INTERVAL 1 DAY), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '07:55:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '18:05:00'), '正常', NOW()),
(6, 6, DATE_SUB(CURDATE(), INTERVAL 1 DAY), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '08:05:00'), DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 1 DAY), INTERVAL 18 HOUR), '正常', NOW())
ON DUPLICATE KEY UPDATE check_in_time = VALUES(check_in_time), check_out_time = VALUES(check_out_time), status = VALUES(status);

INSERT INTO leave_apply (id, user_id, type, start_time, end_time, course_id, course_date, reason, status, approve_remark, create_time, update_time) VALUES
(1, 2, 'sick', DATE_ADD(DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 1 DAY), INTERVAL 9 HOUR), DATE_ADD(DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 1 DAY), INTERVAL 12 HOUR), 2, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 1 DAY), '发热就医，无法参加课程。', '已通过', '情况属实，准假半天。', NOW(), NOW()),
(2, 3, 'personal', DATE_ADD(CURDATE(), INTERVAL 1 DAY), DATE_ADD(DATE_ADD(CURDATE(), INTERVAL 1 DAY), INTERVAL 18 HOUR), NULL, NULL, '参加导师安排的外出调研。', '待审批', NULL, NOW(), NOW()),
(3, 5, 'other', DATE_ADD(DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 3 DAY), INTERVAL 14 HOUR), DATE_ADD(DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 3 DAY), INTERVAL 18 HOUR), 4, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 3 DAY), '学院会议与课程时间冲突。', '已驳回', '证明材料不足，请补充后重新提交。', NOW(), NOW())
ON DUPLICATE KEY UPDATE type = VALUES(type), start_time = VALUES(start_time), end_time = VALUES(end_time), course_id = VALUES(course_id), course_date = VALUES(course_date), reason = VALUES(reason), status = VALUES(status), approve_remark = VALUES(approve_remark), update_time = VALUES(update_time);

-- 智能感知实验室（3间）
INSERT INTO lab (id, name, description, location, capacity, equipment_info, status) VALUES
(1, '智能感知实验室', '支持传感器网络与智能硬件开发', '一教301', 40, '传感器套件,树莓派,Arduino开发板,示波器', 'available'),
(2, '智能感知实验室', '面向5G物联网与边缘计算实验', '二教205', 35, '传感器套件,5G模组,边缘计算网关,示波器', 'available'),
(3, '智能感知实验室', '无线传感器网络与智能识别研究', '三教102', 30, '传感器套件,RFID设备,ZigBee模块,示波器', 'available'),

-- 人工智能实验室（4间）
(4, '人工智能实验室', '支持大规模深度学习训练', '一教401', 40, 'GPU服务器,高性能工作站,投影仪', 'available'),
(5, '人工智能实验室', '专注NLP与文本挖掘研究', '二教301', 35, 'GPU服务器,高性能工作站,白板', 'available'),
(6, '人工智能实验室', '高性能AI计算中心', '三教201', 40, 'GPU服务器,高性能工作站,会议显示屏', 'available'),
(7, '人工智能实验室', '面向教学的AI实验室', '实验楼A301', 30, 'GPU服务器,高性能工作站,投影仪', 'available'),

-- 网络安全实验室（3间）
(8, '网络安全实验室', '网络安全攻防实验环境', '一教302', 40, '防火墙设备,安全靶场平台,服务器集群,投影仪', 'available'),
(9, '网络安全实验室', '安全漏洞研究与密码学实验', '二教301', 35, '防火墙设备,安全靶场平台,服务器集群,白板', 'available'),
(10, '网络安全实验室', '网络流量分析与安全监控', '实验楼A302', 45, '防火墙设备,安全靶场平台,服务器集群,会议显示屏', 'available'),

-- 大数据实验室（4间）
(11, '大数据实验室', 'Hadoop生态与PB级数据处理', '一教501', 40, '服务器集群,高性能工作站,会议显示屏', 'available'),
(12, '大数据实验室', '数据分析与可视化', '二教401', 35, '服务器集群,高性能工作站,投影仪', 'available'),
(13, '大数据实验室', '实时大数据处理与流计算', '三教301', 40, '服务器集群,高性能工作站,白板', 'available'),
(14, '大数据实验室', '数据挖掘与机器学习', '实验楼B201', 30, '服务器集群,高性能工作站,投影仪', 'available'),

-- 云计算实验室（3间）
(15, '云计算实验室', 'IaaS云平台与虚拟化技术', '一教402', 40, '服务器集群,高性能工作站,投影仪', 'available'),
(16, '云计算实验室', '容器与微服务架构', '二教402', 35, '服务器集群,高性能工作站,会议显示屏', 'available'),
(17, '云计算实验室', '云原生应用开发', '实验楼B202', 45, '服务器集群,高性能工作站,白板', 'available'),

-- 软件工程实验室（5间）
(18, '软件工程实验室', '软件开发生命周期管理', '一教201', 40, '高性能工作站,投影仪,白板', 'available'),
(19, '软件工程实验室', '团队协作软件开发', '二教101', 35, '高性能工作站,投影仪,白板', 'available'),
(20, '软件工程实验室', '软件性能测试与优化', '三教201', 40, '高性能工作站,会议显示屏,白板', 'available'),
(21, '软件工程实验室', 'DevOps实践与持续交付', '实验楼A201', 30, '高性能工作站,投影仪,白板', 'available'),
(22, '软件工程实验室', '软件质量保障', '实验楼C301', 35, '高性能工作站,会议显示屏,白板', 'available'),

-- 嵌入式系统实验室（3间）
(23, '嵌入式系统实验室', 'ARM和FPGA开发与硬件调试', '一教101', 40, 'FPGA开发板,示波器,逻辑分析仪,投影仪', 'available'),
(24, '嵌入式系统实验室', '嵌入式Linux与实时系统', '二教102', 35, 'FPGA开发板,示波器,PLC控制器,白板', 'available'),
(25, '嵌入式系统实验室', '嵌入式控制与传感器应用', '实验楼B301', 30, 'FPGA开发板,示波器,传感器套件,投影仪', 'available'),

-- 计算机视觉实验室（4间）
(26, '计算机视觉实验室', '图像采集与处理', '一教403', 40, 'GPU服务器,工业相机,高性能工作站,投影仪', 'available'),
(27, '计算机视觉实验室', '3D视觉与光场成像', '二教501', 35, 'GPU服务器,工业相机,3D打印机,白板', 'available'),
(28, '计算机视觉实验室', '动态视觉与运动分析', '三教302', 40, 'GPU服务器,工业相机,高性能工作站,会议显示屏', 'available'),
(29, '计算机视觉实验室', '工业视觉检测', '实验楼C302', 30, 'GPU服务器,工业相机,高性能工作站,投影仪', 'available'),

-- 移动开发实验室（3间）
(30, '移动开发实验室', 'iOS与Android开发测试', '一教202', 40, '高性能工作站,投影仪,白板', 'available'),
(31, '移动开发实验室', '移动应用兼容性测试', '二教201', 35, '高性能工作站,会议显示屏,白板', 'available'),
(32, '移动开发实验室', '跨平台移动应用开发', '三教401', 30, '高性能工作站,投影仪,白板', 'available'),

-- 虚拟现实实验室（3间）
(33, '虚拟现实实验室', 'VR内容创作与体验', '一教502', 40, 'GPU服务器,高性能工作站,3D打印机,投影仪', 'available'),
(34, '虚拟现实实验室', 'VR/AR/MR混合现实开发', '二教502', 35, 'GPU服务器,高性能工作站,3D打印机,会议显示屏', 'available'),
(35, '虚拟现实实验室', '虚拟现实内容制作', '实验楼C201', 30, 'GPU服务器,高性能工作站,3D打印机,白板', 'available')
ON DUPLICATE KEY UPDATE name = VALUES(name), description = VALUES(description), location = VALUES(location), capacity = VALUES(capacity), equipment_info = VALUES(equipment_info), status = VALUES(status);

INSERT INTO lab_booking (id, lab_id, user_id, date, slot, purpose, status, approve_remark, check_in_time, check_out_time, create_time, update_time) VALUES
(1, 4, 2, DATE_SUB(CURDATE(), INTERVAL 1 DAY), '14:00-16:00', '课题组深度学习模型训练，预计6人使用。', 'completed', '同意使用，请按时归还设备。', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '13:50:00'), DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 1 DAY), INTERVAL 16 HOUR), NOW(), NOW()),
(2, 1, 3, CURDATE(), '16:00-18:00', '物联网传感器实验，预计4人使用。', 'approved', '同意预约。', NULL, NULL, NOW(), NOW()),
(3, 8, 4, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '10:00-12:00', '网络攻防演练准备，预计5人使用。', 'pending', NULL, NULL, NULL, NOW(), NOW()),
(4, 6, 5, DATE_ADD(CURDATE(), INTERVAL 2 DAY), '08:00-10:00', '论文实验复现，需要GPU工作站。', 'rejected', '该时段已有教学安排，请调整预约时间。', NULL, NULL, NOW(), NOW()),
(5, 9, 6, DATE_SUB(CURDATE(), INTERVAL 2 DAY), '08:00-10:00', '安全竞赛训练。', 'no_show', '已批准，逾期未签到。', NULL, NULL, NOW(), NOW())
ON DUPLICATE KEY UPDATE lab_id = VALUES(lab_id), user_id = VALUES(user_id), date = VALUES(date), slot = VALUES(slot), purpose = VALUES(purpose), status = VALUES(status), approve_remark = VALUES(approve_remark), check_in_time = VALUES(check_in_time), check_out_time = VALUES(check_out_time), update_time = VALUES(update_time);

INSERT INTO course (id, course_code, course_name, teacher_id, semester, term_start_date, weeks, weekday, start_time, end_time, location, remark, status, create_time, update_time) VALUES
(1, 'CS5001', '研究方法与论文写作', 8, CONCAT(YEAR(CURDATE()), '-', YEAR(CURDATE()) + 1, '-2'), DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 10 WEEK), '1-16', 1, '08:00:00', '10:00:00', '教学楼A201', '研究生核心课程', 'active', NOW(), NOW()),
(2, 'CS5002', '高级数据库系统', 9, CONCAT(YEAR(CURDATE()), '-', YEAR(CURDATE()) + 1, '-2'), DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 10 WEEK), '1-16', 2, '10:00:00', '12:00:00', '教学楼B305', '案例与实验结合', 'active', NOW(), NOW()),
(3, 'CS5003', '机器学习专题', 10, CONCAT(YEAR(CURDATE()), '-', YEAR(CURDATE()) + 1, '-2'), DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 10 WEEK), '1-16', 3, '14:00:00', '16:00:00', '实验楼C402', '含课程实践', 'active', NOW(), NOW()),
(4, 'CS5004', '分布式系统设计', 8, CONCAT(YEAR(CURDATE()), '-', YEAR(CURDATE()) + 1, '-2'), DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 10 WEEK), '1-16', 4, '16:00:00', '18:00:00', '教学楼A305', '面向软件方向学生', 'active', NOW(), NOW()),
(5, 'CS5005', '科研伦理与学术规范', 9, CONCAT(YEAR(CURDATE()), '-', YEAR(CURDATE()) + 1, '-2'), DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 10 WEEK), '1-8', 5, '08:00:00', '10:00:00', '教学楼C101', '公共必修课', 'inactive', NOW(), NOW())
ON DUPLICATE KEY UPDATE course_name = VALUES(course_name), teacher_id = VALUES(teacher_id), semester = VALUES(semester), term_start_date = VALUES(term_start_date), weeks = VALUES(weeks), weekday = VALUES(weekday), start_time = VALUES(start_time), end_time = VALUES(end_time), location = VALUES(location), remark = VALUES(remark), status = VALUES(status), update_time = VALUES(update_time);

INSERT INTO course_student (course_id, student_id, create_time) VALUES
(1, 2, NOW()), (1, 3, NOW()), (1, 4, NOW()), (1, 5, NOW()),
(2, 2, NOW()), (2, 5, NOW()), (2, 6, NOW()), (2, 7, NOW()),
(3, 2, NOW()), (3, 4, NOW()), (3, 6, NOW()), (3, 7, NOW()),
(4, 3, NOW()), (4, 4, NOW()), (4, 5, NOW()), (4, 6, NOW())
ON DUPLICATE KEY UPDATE create_time = VALUES(create_time);

INSERT INTO course_attendance (id, course_id, student_id, course_date, status, check_in_time, remark, create_time, update_time) VALUES
(1, 1, 2, DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), '已签到', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), '07:56:00'), NULL, NOW(), NOW()),
(2, 1, 3, DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), '迟到', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), '08:12:00'), '到课迟到12分钟', NOW(), NOW()),
(3, 1, 4, DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), '缺勤', NULL, '未参加课程签到', NOW(), NOW()),
(4, 2, 2, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 1 DAY), '请假', NULL, '已批准课程请假', NOW(), NOW()),
(5, 2, 5, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 1 DAY), '已签到', TIMESTAMP(DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 1 DAY), '09:58:00'), NULL, NOW(), NOW()),
(6, 3, 4, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 2 DAY), '已签到', TIMESTAMP(DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 2 DAY), '13:55:00'), NULL, NOW(), NOW()),
(7, 4, 3, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 3 DAY), '待签到', NULL, NULL, NOW(), NOW())
ON DUPLICATE KEY UPDATE course_id = VALUES(course_id), student_id = VALUES(student_id), course_date = VALUES(course_date), status = VALUES(status), check_in_time = VALUES(check_in_time), remark = VALUES(remark), update_time = VALUES(update_time);

-- ============================================================
-- 10. 扩充样例数据 - 使系统数据更加丰富真实
-- ============================================================

-- ---------- 补充考勤记录（覆盖更多学生、更多天数） ----------
INSERT INTO attendance_record (user_id, date, check_in_time, check_out_time, status, create_time) VALUES
-- 前一周周一
(2, DATE_SUB(CURDATE(), INTERVAL 7 DAY), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY), '07:52:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY), '18:10:00'), '正常', NOW()),
(3, DATE_SUB(CURDATE(), INTERVAL 7 DAY), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY), '07:58:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY), '18:00:00'), '正常', NOW()),
(4, DATE_SUB(CURDATE(), INTERVAL 7 DAY), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY), '08:20:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY), '17:45:00'), '迟到', NOW()),
(5, DATE_SUB(CURDATE(), INTERVAL 7 DAY), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY), '07:50:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY), '18:05:00'), '正常', NOW()),
(6, DATE_SUB(CURDATE(), INTERVAL 7 DAY), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY), '08:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY), '16:20:00'), '早退', NOW()),
(7, DATE_SUB(CURDATE(), INTERVAL 7 DAY), NULL, NULL, '缺勤', NOW()),
-- 前一周周二
(2, DATE_SUB(CURDATE(), INTERVAL 6 DAY), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '07:48:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '18:15:00'), '正常', NOW()),
(3, DATE_SUB(CURDATE(), INTERVAL 6 DAY), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '08:25:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '18:00:00'), '迟到', NOW()),
(4, DATE_SUB(CURDATE(), INTERVAL 6 DAY), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '07:55:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '18:02:00'), '正常', NOW()),
(5, DATE_SUB(CURDATE(), INTERVAL 6 DAY), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '07:59:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '18:00:00'), '正常', NOW()),
(6, DATE_SUB(CURDATE(), INTERVAL 6 DAY), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '08:01:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '18:10:00'), '正常', NOW()),
(7, DATE_SUB(CURDATE(), INTERVAL 6 DAY), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '08:05:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '18:00:00'), '正常', NOW()),
-- 前一周周三
(2, DATE_SUB(CURDATE(), INTERVAL 5 DAY), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '07:50:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '18:00:00'), '正常', NOW()),
(3, DATE_SUB(CURDATE(), INTERVAL 5 DAY), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '07:55:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '18:05:00'), '正常', NOW()),
(4, DATE_SUB(CURDATE(), INTERVAL 5 DAY), NULL, NULL, '缺勤', NOW()),
(5, DATE_SUB(CURDATE(), INTERVAL 5 DAY), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '08:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '18:00:00'), '正常', NOW()),
(6, DATE_SUB(CURDATE(), INTERVAL 5 DAY), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '07:45:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '18:20:00'), '正常', NOW()),
(7, DATE_SUB(CURDATE(), INTERVAL 5 DAY), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '08:30:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '18:00:00'), '迟到', NOW()),
-- 前一周周四
(2, DATE_SUB(CURDATE(), INTERVAL 4 DAY), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '07:58:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '18:00:00'), '正常', NOW()),
(3, DATE_SUB(CURDATE(), INTERVAL 4 DAY), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '08:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '18:00:00'), '正常', NOW()),
(4, DATE_SUB(CURDATE(), INTERVAL 4 DAY), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '07:50:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '18:10:00'), '正常', NOW()),
(5, DATE_SUB(CURDATE(), INTERVAL 4 DAY), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '08:15:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '18:00:00'), '迟到', NOW()),
(6, DATE_SUB(CURDATE(), INTERVAL 4 DAY), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '07:55:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '16:00:00'), '早退', NOW()),
(7, DATE_SUB(CURDATE(), INTERVAL 4 DAY), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '07:52:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '18:05:00'), '正常', NOW()),
-- 前一周周五
(2, DATE_SUB(CURDATE(), INTERVAL 3 DAY), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '07:55:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '17:30:00'), '早退', NOW()),
(3, DATE_SUB(CURDATE(), INTERVAL 3 DAY), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '08:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '18:00:00'), '正常', NOW()),
(4, DATE_SUB(CURDATE(), INTERVAL 3 DAY), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '07:58:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '18:00:00'), '正常', NOW()),
(5, DATE_SUB(CURDATE(), INTERVAL 3 DAY), NULL, NULL, '缺勤', NOW()),
(6, DATE_SUB(CURDATE(), INTERVAL 3 DAY), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '08:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '18:00:00'), '正常', NOW()),
(7, DATE_SUB(CURDATE(), INTERVAL 3 DAY), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '07:50:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '18:10:00'), '正常', NOW())
ON DUPLICATE KEY UPDATE check_in_time = VALUES(check_in_time), check_out_time = VALUES(check_out_time), status = VALUES(status);

-- ---------- 补充请假申请（更多类型和状态，确保 course_date 与课程 weekday 匹配） ----------
INSERT INTO leave_apply (user_id, type, start_time, end_time, course_id, course_date, reason, status, approve_remark, create_time, update_time) VALUES
-- 课程请假（已通过）- 上周三课程3
(4, 'sick', TIMESTAMP(DATE_ADD(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 2 DAY), '08:00:00'), TIMESTAMP(DATE_ADD(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 2 DAY), '18:00:00'), 3, DATE_ADD(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 2 DAY), '身体不适，需要休息一天。', '已通过', '注意休息，尽快恢复。', NOW(), NOW()),
-- 课程请假（已通过）- 本周四课程4
(5, 'personal', TIMESTAMP(DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 3 DAY), '14:00:00'), DATE_ADD(TIMESTAMP(DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 3 DAY), '14:00:00'), INTERVAL 4 HOUR), 4, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 3 DAY), '家中有急事需要处理。', '已通过', '准假半天。', NOW(), NOW()),
-- 非课程请假（已通过）- 连续两天
(7, 'sick', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY), '08:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '18:00:00'), NULL, NULL, '感冒发烧，需要请假两天。', '已通过', '请注意身体，及时就医。', NOW(), NOW()),
-- 非课程请假（待审批）- 未来
(2, 'personal', TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 2 DAY), '08:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 2 DAY), '18:00:00'), NULL, NULL, '参加学术会议。', '待审批', NULL, NOW(), NOW()),
-- 非课程请假（已通过）- 过去
(6, 'sick', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '08:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '18:00:00'), NULL, NULL, '肠胃不适，需要休息。', '已通过', '注意饮食。', NOW(), NOW()),
-- 课程请假（待审批）- 下周三课程3
(3, 'other', TIMESTAMP(DATE_ADD(DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 2 DAY), '14:00:00'), DATE_ADD(TIMESTAMP(DATE_ADD(DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 2 DAY), '14:00:00'), INTERVAL 4 HOUR), 3, DATE_ADD(DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 2 DAY), '参加校级科技竞赛决赛。', '待审批', NULL, NOW(), NOW()),
-- 非课程请假（待审批）- 未来连续两天
(4, 'personal', TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 5 DAY), '08:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 6 DAY), '18:00:00'), NULL, NULL, '回家办理证件。', '待审批', NULL, NOW(), NOW()),
-- 课程请假（已通过）- 本周一课程1
(7, 'other', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), '08:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), '12:00:00'), 1, DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), '导师安排参加项目答辩。', '已通过', '已确认，准假。', NOW(), NOW()),
-- 课程请假（已驳回）- 上周四课程4
(6, 'personal', TIMESTAMP(DATE_ADD(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 3 DAY), '16:00:00'), DATE_ADD(TIMESTAMP(DATE_ADD(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 3 DAY), '16:00:00'), INTERVAL 2 HOUR), 4, DATE_ADD(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 3 DAY), '临时有事需要请假。', '已驳回', '理由不充分，请提供详细说明。', NOW(), NOW()),
-- 课程请假（已通过）- 上周二课程2
(6, 'sick', TIMESTAMP(DATE_ADD(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 1 DAY), '10:00:00'), DATE_ADD(TIMESTAMP(DATE_ADD(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 1 DAY), '10:00:00'), INTERVAL 2 HOUR), 2, DATE_ADD(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 1 DAY), '突发头痛，无法上课。', '已通过', '注意休息。', NOW(), NOW())
ON DUPLICATE KEY UPDATE type = VALUES(type), start_time = VALUES(start_time), end_time = VALUES(end_time), course_id = VALUES(course_id), course_date = VALUES(course_date), reason = VALUES(reason), status = VALUES(status), approve_remark = VALUES(approve_remark), update_time = VALUES(update_time);

-- ---------- 补充实验室预约（覆盖全部7种状态，含维护中实验室被拒、时段冲突等场景） ----------
INSERT INTO lab_booking (lab_id, user_id, date, slot, purpose, status, approve_remark, check_in_time, check_out_time, create_time, update_time) VALUES
-- completed：已完成的预约
(1, 3, DATE_SUB(CURDATE(), INTERVAL 3 DAY), '08:00-10:00', '深度学习模型调参实验，3人使用。', 'completed', '同意。', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '07:55:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '09:50:00'), NOW(), NOW()),
(2, 4, DATE_SUB(CURDATE(), INTERVAL 3 DAY), '10:00-12:00', '嵌入式系统课程实验，5人使用。', 'completed', '注意设备归还。', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '09:58:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '11:55:00'), NOW(), NOW()),
(3, 7, DATE_SUB(CURDATE(), INTERVAL 4 DAY), '14:00-16:00', 'CTF竞赛训练，4人使用。', 'completed', '同意，注意网络隔离。', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '13:50:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '15:58:00'), NOW(), NOW()),
(1, 6, DATE_SUB(CURDATE(), INTERVAL 5 DAY), '16:00-18:00', 'NLP实验数据预处理，2人使用。', 'completed', '同意使用。', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '15:55:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '17:50:00'), NOW(), NOW()),
-- checked_in：正在使用中
(1, 2, CURDATE(), '08:00-10:00', '论文实验GPU训练任务，需要长时间运行。', 'checked_in', '同意，注意散热。', TIMESTAMP(CURDATE(), '07:58:00'), NULL, NOW(), NOW()),
-- pending：待审批
(2, 5, DATE_ADD(CURDATE(), INTERVAL 2 DAY), '08:00-10:00', '传感器数据采集实验，3人使用。', 'pending', NULL, NULL, NULL, NOW(), NOW()),
(3, 2, DATE_ADD(CURDATE(), INTERVAL 2 DAY), '14:00-16:00', '渗透测试学习，2人使用。', 'pending', NULL, NULL, NULL, NOW(), NOW()),
(1, 7, DATE_ADD(CURDATE(), INTERVAL 3 DAY), '10:00-12:00', '目标检测模型训练，4人使用。', 'pending', NULL, NULL, NULL, NOW(), NOW()),
-- approved：已通过（未来预约）
(2, 6, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '10:00-12:00', '物联网网关调试，3人使用。', 'approved', '同意预约，请爱护设备。', NULL, NULL, NOW(), NOW()),
(1, 4, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '14:00-16:00', '图像分割算法验证，2人使用。', 'approved', '同意。', NULL, NULL, NOW(), NOW()),
-- rejected：维护中实验室被拒
(4, 3, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '08:00-10:00', '数据可视化实验。', 'rejected', '实验室正在维护中，暂停预约，预计下月恢复。', NULL, NULL, NOW(), NOW()),
-- cancelled：用户主动取消
(3, 5, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '16:00-18:00', '安全工具测试。', 'cancelled', NULL, NULL, NULL, NOW(), NOW()),
-- no_show：逾期未签到
(2, 7, DATE_SUB(CURDATE(), INTERVAL 2 DAY), '10:00-12:00', '物联网平台联调测试。', 'no_show', '已批准，逾期未签到。', NULL, NULL, NOW(), NOW())
ON DUPLICATE KEY UPDATE lab_id = VALUES(lab_id), user_id = VALUES(user_id), date = VALUES(date), slot = VALUES(slot), purpose = VALUES(purpose), status = VALUES(status), approve_remark = VALUES(approve_remark), check_in_time = VALUES(check_in_time), check_out_time = VALUES(check_out_time), update_time = VALUES(update_time);

-- ---------- 补充选课关系（让课程5也有学生） ----------
INSERT INTO course_student (course_id, student_id, create_time) VALUES
(5, 2, NOW()), (5, 3, NOW()), (5, 4, NOW()), (5, 5, NOW()), (5, 6, NOW()), (5, 7, NOW()),
(3, 3, NOW()), (3, 5, NOW()),
(4, 7, NOW()), (4, 2, NOW()),
(1, 6, NOW()), (1, 7, NOW())
ON DUPLICATE KEY UPDATE create_time = VALUES(create_time);

-- ---------- 补充课程考勤（覆盖更多课程和日期） ----------
INSERT INTO course_attendance (course_id, student_id, course_date, status, check_in_time, remark, create_time, update_time) VALUES
-- 课程1（周一）- 上上周
(1, 2, DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), '已签到', TIMESTAMP(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), '07:55:00'), NULL, NOW(), NOW()),
(1, 3, DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), '已签到', TIMESTAMP(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), '07:58:00'), NULL, NOW(), NOW()),
(1, 4, DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), '迟到', TIMESTAMP(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), '08:20:00'), '迟到20分钟', NOW(), NOW()),
(1, 5, DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), '已签到', TIMESTAMP(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), '07:50:00'), NULL, NOW(), NOW()),
(1, 6, DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), '缺勤', NULL, '未到课', NOW(), NOW()),
(1, 7, DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), '请假', NULL, '导师安排项目答辩', NOW(), NOW()),
-- 课程2（周二）- 本周
(2, 2, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 1 DAY), '请假', NULL, '已批准病假', NOW(), NOW()),
(2, 5, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 1 DAY), '已签到', TIMESTAMP(DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 1 DAY), '09:55:00'), NULL, NOW(), NOW()),
(2, 6, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 1 DAY), '已签到', TIMESTAMP(DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 1 DAY), '09:58:00'), NULL, NOW(), NOW()),
(2, 7, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 1 DAY), '迟到', TIMESTAMP(DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 1 DAY), '10:15:00'), '迟到15分钟', NOW(), NOW()),
-- 课程2（周二）- 上周
(2, 2, DATE_ADD(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 1 DAY), '已签到', TIMESTAMP(DATE_ADD(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 1 DAY), '09:50:00'), NULL, NOW(), NOW()),
(2, 5, DATE_ADD(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 1 DAY), '已签到', TIMESTAMP(DATE_ADD(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 1 DAY), '09:58:00'), NULL, NOW(), NOW()),
(2, 6, DATE_ADD(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 1 DAY), '缺勤', NULL, '未到课未请假', NOW(), NOW()),
(2, 7, DATE_ADD(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 1 DAY), '已签到', TIMESTAMP(DATE_ADD(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 1 DAY), '10:00:00'), NULL, NOW(), NOW()),
-- 课程3（周三）- 本周
(3, 2, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 2 DAY), '已签到', TIMESTAMP(DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 2 DAY), '13:50:00'), NULL, NOW(), NOW()),
(3, 3, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 2 DAY), '已签到', TIMESTAMP(DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 2 DAY), '13:55:00'), NULL, NOW(), NOW()),
(3, 5, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 2 DAY), '缺勤', NULL, '未到课', NOW(), NOW()),
(3, 6, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 2 DAY), '已签到', TIMESTAMP(DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 2 DAY), '13:58:00'), NULL, NOW(), NOW()),
(3, 7, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 2 DAY), '迟到', TIMESTAMP(DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 2 DAY), '14:18:00'), '迟到18分钟', NOW(), NOW()),
-- 课程4（周四）- 本周
(4, 3, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 3 DAY), '已签到', TIMESTAMP(DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 3 DAY), '15:55:00'), NULL, NOW(), NOW()),
(4, 4, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 3 DAY), '已签到', TIMESTAMP(DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 3 DAY), '15:58:00'), NULL, NOW(), NOW()),
(4, 5, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 3 DAY), '请假', NULL, '已批准请假', NOW(), NOW()),
(4, 6, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 3 DAY), '已签到', TIMESTAMP(DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 3 DAY), '15:50:00'), NULL, NOW(), NOW()),
(4, 7, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 3 DAY), '缺勤', NULL, '未到课未请假', NOW(), NOW()),
(4, 2, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 3 DAY), '已签到', TIMESTAMP(DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 3 DAY), '15:52:00'), NULL, NOW(), NOW()),
-- 课程4（周四）- 上周（包含请假被驳回后缺勤的情况）
(4, 3, DATE_ADD(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 3 DAY), '已签到', TIMESTAMP(DATE_ADD(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 3 DAY), '15:52:00'), NULL, NOW(), NOW()),
(4, 4, DATE_ADD(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 3 DAY), '已签到', TIMESTAMP(DATE_ADD(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 3 DAY), '15:55:00'), NULL, NOW(), NOW()),
(4, 5, DATE_ADD(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 3 DAY), '已签到', TIMESTAMP(DATE_ADD(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 3 DAY), '15:58:00'), NULL, NOW(), NOW()),
(4, 6, DATE_ADD(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 3 DAY), '缺勤', NULL, '请假被驳回后未到课', NOW(), NOW()),
(4, 2, DATE_ADD(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 3 DAY), '迟到', TIMESTAMP(DATE_ADD(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 3 DAY), '16:10:00'), '迟到10分钟', NOW(), NOW()),
(4, 7, DATE_ADD(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 3 DAY), '已签到', TIMESTAMP(DATE_ADD(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 3 DAY), '15:50:00'), NULL, NOW(), NOW()),
-- 课程3（周三）- 上周（包含请假通过的情况）
(3, 2, DATE_ADD(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 2 DAY), '已签到', TIMESTAMP(DATE_ADD(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 2 DAY), '13:52:00'), NULL, NOW(), NOW()),
(3, 3, DATE_ADD(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 2 DAY), '已签到', TIMESTAMP(DATE_ADD(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 2 DAY), '13:58:00'), NULL, NOW(), NOW()),
(3, 4, DATE_ADD(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 2 DAY), '请假', NULL, '已批准病假', NOW(), NOW()),
(3, 5, DATE_ADD(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 2 DAY), '已签到', TIMESTAMP(DATE_ADD(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 2 DAY), '13:55:00'), NULL, NOW(), NOW()),
(3, 6, DATE_ADD(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 2 DAY), '迟到', TIMESTAMP(DATE_ADD(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 2 DAY), '14:15:00'), '迟到15分钟', NOW(), NOW()),
(3, 7, DATE_ADD(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 2 DAY), '已签到', TIMESTAMP(DATE_ADD(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 2 DAY), '13:50:00'), NULL, NOW(), NOW()),
-- 课程2（周二）- 上周（包含请假通过的情况）
(2, 6, DATE_ADD(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 1 DAY), '请假', NULL, '已批准病假', NOW(), NOW()),
-- 课程5（周五）- 上周（inactive课程也有历史考勤）
(5, 2, DATE_ADD(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 4 DAY), '已签到', TIMESTAMP(DATE_ADD(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 4 DAY), '07:55:00'), NULL, NOW(), NOW()),
(5, 3, DATE_ADD(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 4 DAY), '已签到', TIMESTAMP(DATE_ADD(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 4 DAY), '07:58:00'), NULL, NOW(), NOW()),
(5, 4, DATE_ADD(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 4 DAY), '迟到', TIMESTAMP(DATE_ADD(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 4 DAY), '08:12:00'), '迟到12分钟', NOW(), NOW()),
(5, 5, DATE_ADD(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 4 DAY), '缺勤', NULL, '未到课', NOW(), NOW()),
(5, 6, DATE_ADD(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 4 DAY), '已签到', TIMESTAMP(DATE_ADD(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 4 DAY), '08:00:00'), NULL, NOW(), NOW()),
(5, 7, DATE_ADD(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 4 DAY), '已签到', TIMESTAMP(DATE_ADD(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 4 DAY), '07:50:00'), NULL, NOW(), NOW()),
-- 课程1（周一）- 本周 补充更多学生
(1, 5, DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), '已签到', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), '07:50:00'), NULL, NOW(), NOW()),
(1, 6, DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), '已签到', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), '07:58:00'), NULL, NOW(), NOW()),
(1, 7, DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), '请假', NULL, '导师安排项目答辩', NOW(), NOW())
ON DUPLICATE KEY UPDATE status = VALUES(status), check_in_time = VALUES(check_in_time), remark = VALUES(remark), update_time = VALUES(update_time);

-- ============================================================
-- 11. 补充更多测试场景数据（便于全面测试录屏）
-- ============================================================

-- ---------- 补充更多学生用户（扩展到10个学生） ----------
INSERT INTO sys_user (id, username, password, name, phone, email, role, status, create_time, update_time) VALUES
(11, '2024007', '$2a$10$QGx.JbaZNz5Ho5CLv5N82eMtGnKGWR/FAFcTc3fy/v25e49wMFsFy', '孙静', '13800000007', '2024007@stu.lamp.edu.cn', 'student', 1, NOW(), NOW()),
(12, '2024008', '$2a$10$QGx.JbaZNz5Ho5CLv5N82eMtGnKGWR/FAFcTc3fy/v25e49wMFsFy', '周杰', '13800000008', '2024008@stu.lamp.edu.cn', 'student', 1, NOW(), NOW()),
(13, '2024009', '$2a$10$QGx.JbaZNz5Ho5CLv5N82eMtGnKGWR/FAFcTc3fy/v25e49wMFsFy', '吴倩', '13800000009', '2024009@stu.lamp.edu.cn', 'student', 1, NOW(), NOW()),
(14, '2024010', '$2a$10$QGx.JbaZNz5Ho5CLv5N82eMtGnKGWR/FAFcTc3fy/v25e49wMFsFy', '郑浩', '13800000010', '2024010@stu.lamp.edu.cn', 'student', 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE password = VALUES(password), name = VALUES(name), phone = VALUES(phone), email = VALUES(email), role = VALUES(role), status = VALUES(status), update_time = VALUES(update_time);

-- ---------- 新增学生的选课关系 ----------
INSERT INTO course_student (course_id, student_id, create_time) VALUES
(1, 11, NOW()), (1, 12, NOW()), (1, 13, NOW()),
(2, 11, NOW()), (2, 12, NOW()),
(3, 11, NOW()), (3, 13, NOW()),
(4, 11, NOW()), (4, 12, NOW()), (4, 13, NOW()),
(5, 11, NOW()), (5, 12, NOW()), (5, 13, NOW())
ON DUPLICATE KEY UPDATE create_time = VALUES(create_time);

-- ---------- 新增学生的日常考勤记录 ----------
INSERT INTO attendance_record (user_id, date, check_in_time, check_out_time, status, create_time) VALUES
-- 新学生 前一周
(11, DATE_SUB(CURDATE(), INTERVAL 7 DAY), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY), '07:55:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY), '18:00:00'), '正常', NOW()),
(12, DATE_SUB(CURDATE(), INTERVAL 7 DAY), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY), '08:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY), '18:05:00'), '正常', NOW()),
(13, DATE_SUB(CURDATE(), INTERVAL 7 DAY), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY), '08:10:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY), '18:00:00'), '迟到', NOW()),
(11, DATE_SUB(CURDATE(), INTERVAL 6 DAY), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '07:50:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '18:10:00'), '正常', NOW()),
(12, DATE_SUB(CURDATE(), INTERVAL 6 DAY), NULL, NULL, '缺勤', NOW()),
(13, DATE_SUB(CURDATE(), INTERVAL 6 DAY), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '07:58:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '18:00:00'), '正常', NOW()),
(11, DATE_SUB(CURDATE(), INTERVAL 5 DAY), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '07:55:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '17:00:00'), '早退', NOW()),
(12, DATE_SUB(CURDATE(), INTERVAL 5 DAY), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '08:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '18:00:00'), '正常', NOW()),
(13, DATE_SUB(CURDATE(), INTERVAL 5 DAY), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '07:52:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '18:05:00'), '正常', NOW())
ON DUPLICATE KEY UPDATE check_in_time = VALUES(check_in_time), check_out_time = VALUES(check_out_time), status = VALUES(status);

-- ---------- 新增学生的课程考勤 ----------
INSERT INTO course_attendance (course_id, student_id, course_date, status, check_in_time, remark, create_time, update_time) VALUES
-- 课程1 新学生
(1, 11, DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), '已签到', TIMESTAMP(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), '07:52:00'), NULL, NOW(), NOW()),
(1, 12, DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), '缺勤', NULL, '未到课', NOW(), NOW()),
(1, 13, DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), '已签到', TIMESTAMP(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), '07:58:00'), NULL, NOW(), NOW()),
(1, 11, DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), '迟到', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), '08:15:00'), '迟到15分钟', NOW(), NOW()),
(1, 12, DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), '已签到', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), '07:55:00'), NULL, NOW(), NOW()),
(1, 13, DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), '已签到', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), '07:50:00'), NULL, NOW(), NOW()),
-- 课程2 新学生
(2, 11, DATE_ADD(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 1 DAY), '已签到', TIMESTAMP(DATE_ADD(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 1 DAY), '09:55:00'), NULL, NOW(), NOW()),
(2, 12, DATE_ADD(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 1 DAY), '迟到', TIMESTAMP(DATE_ADD(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 1 DAY), '10:20:00'), '迟到20分钟', NOW(), NOW()),
(2, 11, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 1 DAY), '已签到', TIMESTAMP(DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 1 DAY), '09:58:00'), NULL, NOW(), NOW()),
(2, 12, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 1 DAY), '已签到', TIMESTAMP(DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 1 DAY), '09:52:00'), NULL, NOW(), NOW()),
-- 课程3 新学生
(3, 11, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 2 DAY), '已签到', TIMESTAMP(DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 2 DAY), '13:52:00'), NULL, NOW(), NOW()),
(3, 13, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 2 DAY), '已签到', TIMESTAMP(DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 2 DAY), '13:58:00'), NULL, NOW(), NOW()),
-- 课程4 新学生
(4, 11, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 3 DAY), '已签到', TIMESTAMP(DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 3 DAY), '15:55:00'), NULL, NOW(), NOW()),
(4, 12, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 3 DAY), '缺勤', NULL, '未到课', NOW(), NOW()),
(4, 13, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 3 DAY), '已签到', TIMESTAMP(DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 3 DAY), '15:50:00'), NULL, NOW(), NOW())
ON DUPLICATE KEY UPDATE status = VALUES(status), check_in_time = VALUES(check_in_time), remark = VALUES(remark), update_time = VALUES(update_time);

-- ---------- 新增学生的实验室预约 ----------
INSERT INTO lab_booking (lab_id, user_id, date, slot, purpose, status, approve_remark, check_in_time, check_out_time, create_time, update_time) VALUES
(2, 11, DATE_SUB(CURDATE(), INTERVAL 6 DAY), '14:00-16:00', '物联网课程实验，3人小组。', 'completed', '同意。', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '13:55:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '15:50:00'), NOW(), NOW()),
(3, 12, DATE_SUB(CURDATE(), INTERVAL 5 DAY), '10:00-12:00', '网络安全实验，2人使用。', 'completed', '同意，注意安全。', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '09:58:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '11:55:00'), NOW(), NOW()),
(1, 13, DATE_ADD(CURDATE(), INTERVAL 2 DAY), '14:00-16:00', '机器学习模型训练，需要GPU。', 'pending', NULL, NULL, NULL, NOW(), NOW()),
(2, 11, DATE_ADD(CURDATE(), INTERVAL 3 DAY), '08:00-10:00', '传感器调试实验。', 'approved', '同意预约。', NULL, NULL, NOW(), NOW())
ON DUPLICATE KEY UPDATE lab_id = VALUES(lab_id), user_id = VALUES(user_id), date = VALUES(date), slot = VALUES(slot), purpose = VALUES(purpose), status = VALUES(status), approve_remark = VALUES(approve_remark), check_in_time = VALUES(check_in_time), check_out_time = VALUES(check_out_time), update_time = VALUES(update_time);

-- ---------- 新增学生的请假申请 ----------
INSERT INTO leave_apply (user_id, type, start_time, end_time, course_id, course_date, reason, status, approve_remark, create_time, update_time) VALUES
(11, 'sick', TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 4 DAY), '08:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 4 DAY), '18:00:00'), NULL, NULL, '身体不适，需要就医。', '待审批', NULL, NOW(), NOW()),
(12, 'personal', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '08:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '18:00:00'), NULL, NULL, '家中有事。', '已通过', '准假一天。', NOW(), NOW()),
(13, 'other', TIMESTAMP(DATE_ADD(DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 2 DAY), '14:00:00'), DATE_ADD(TIMESTAMP(DATE_ADD(DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 2 DAY), '14:00:00'), INTERVAL 2 HOUR), 3, DATE_ADD(DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY), INTERVAL 2 DAY), '参加学术讲座。', '待审批', NULL, NOW(), NOW())
ON DUPLICATE KEY UPDATE type = VALUES(type), start_time = VALUES(start_time), end_time = VALUES(end_time), course_id = VALUES(course_id), course_date = VALUES(course_date), reason = VALUES(reason), status = VALUES(status), approve_remark = VALUES(approve_remark), update_time = VALUES(update_time);

-- ---------- 补充更多实验室预约（覆盖不同时段和场景） ----------
INSERT INTO lab_booking (lab_id, user_id, date, slot, purpose, status, approve_remark, check_in_time, check_out_time, create_time, update_time) VALUES
-- 同一天不同时段的预约
(1, 5, DATE_SUB(CURDATE(), INTERVAL 1 DAY), '10:00-12:00', '算法验证实验。', 'completed', '同意。', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '09:58:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '11:50:00'), NOW(), NOW()),
(2, 3, DATE_SUB(CURDATE(), INTERVAL 1 DAY), '08:00-10:00', '物联网设备测试。', 'completed', '同意。', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '07:55:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '09:55:00'), NOW(), NOW()),
(3, 6, DATE_SUB(CURDATE(), INTERVAL 1 DAY), '16:00-18:00', '安全漏洞扫描实验。', 'completed', '同意。', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '15:58:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '17:55:00'), NOW(), NOW()),
-- 未来多个待审批预约
(1, 3, DATE_ADD(CURDATE(), INTERVAL 4 DAY), '08:00-10:00', '深度学习实验。', 'pending', NULL, NULL, NULL, NOW(), NOW()),
(2, 4, DATE_ADD(CURDATE(), INTERVAL 4 DAY), '10:00-12:00', '嵌入式开发。', 'pending', NULL, NULL, NULL, NOW(), NOW()),
(3, 5, DATE_ADD(CURDATE(), INTERVAL 5 DAY), '14:00-16:00', '网络攻防演练。', 'pending', NULL, NULL, NULL, NOW(), NOW()),
-- 已通过但未签到的预约
(1, 6, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '10:00-12:00', 'AI模型训练。', 'approved', '同意预约。', NULL, NULL, NOW(), NOW()),
(3, 7, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '08:00-10:00', '安全工具使用培训。', 'approved', '同意。', NULL, NULL, NOW(), NOW())
ON DUPLICATE KEY UPDATE lab_id = VALUES(lab_id), user_id = VALUES(user_id), date = VALUES(date), slot = VALUES(slot), purpose = VALUES(purpose), status = VALUES(status), approve_remark = VALUES(approve_remark), check_in_time = VALUES(check_in_time), check_out_time = VALUES(check_out_time), update_time = VALUES(update_time);
