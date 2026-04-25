# 研究生考勤及实验室预约系统后端

基于 `Spring Boot 2.7 + JPA + MySQL` 的后端服务，对接前端项目 [lamp_front](../lamp_front)。

当前代码现状已经收敛为 3 个业务角色：
- `student`：研究生
- `teacher`：教师
- `admin`：实验室管理员

其中 `admin` 已不再承担课程管理、考勤总览、用户管理等系统级职责，只保留实验室相关后台能力。

## 技术栈

- Java 8
- Spring Boot 2.7.18
- Spring Data JPA
- MySQL 8
- H2（可选开发库）
- JWT（`jjwt 0.11.5`）
- Lombok

## 当前业务模块

### 认证与个人中心
- 登录、注册
- 获取当前登录用户
- 修改密码
- 更新个人资料

### 课程与考勤
- 学生课表、教师授课表
- 学生课程签到
- 学生课程考勤记录
- 学生课程请假、撤销请假
- 教师按授课课程审批请假
- 教师课程考勤管理

### 实验室预约
- 实验室列表、详情、时段查询
- 学生/教师提交预约
- 我的预约
- 预约取消
- 预约签到、签退

### 实验室管理员后台
- 预约审批
- 实验室管理
- 实验室使用统计
- 工作台实验室统计卡片

## 角色边界

### `student`
- 课程签到
- 课程考勤
- 课程请假 / 我的请假
- 实验室预约 / 我的预约

### `teacher`
- 我的授课
- 课程考勤管理
- 请假审批
- 实验室预约 / 我的预约

### `admin`
- 实验室列表
- 预约审批
- 实验室管理
- 实验室使用统计
- 个人资料 / 修改密码

当前 `admin` 无法访问：
- 课表管理
- 考勤总览
- 用户管理
- 全局课程考勤与课程学生名单查看

## 接口约定

- 服务端口：`8080`
- 上下文路径：`/api`
- 完整接口示例：`http://localhost:8080/api/auth/login`
- 前端开发环境通过 Vite 代理访问 `/api`

统一响应格式：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

除以下接口外，其余接口都需要携带 JWT：
- `POST /api/auth/login`
- `POST /api/auth/register`

请求头格式：

```http
Authorization: Bearer <token>
```

## 默认账号

首次启动时，如果数据库中不存在 `admin`，系统会自动创建：

- 用户名：`admin`
- 密码：`admin123`
- 角色：`admin`（实验室管理员）

## 运行方式

### 1. 准备数据库

推荐先执行建表与初始化脚本：

```bash
mysql -u root -p < docs/database.sql
```

也可以直接依赖 JPA 自动更新表结构，当前配置为：

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update
```

### 2. 修改数据库连接

编辑 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/lamp?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&createDatabaseIfNotExist=true
    username: root
    password: 你的数据库密码
```

### 3. 启动项目

```bash
mvn spring-boot:run
```

或先编译再启动：

```bash
mvn clean package
java -jar target/lamp-back-1.0.0.jar
```

启动后访问：
- 服务地址：`http://localhost:8080`
- 接口前缀：`http://localhost:8080/api`

### 4. 使用 H2（可选）

`application.yml` 中保留了 H2 示例配置。开发时如果不想连接 MySQL，可以注释 MySQL 配置并启用 H2 配置。

## 主要接口

### 认证
- `POST /auth/login`
- `POST /auth/register`
- `GET /auth/user`
- `PUT /auth/password`
- `PUT /user/profile`

### 课程与考勤
- `GET /course/student/schedule`
- `GET /course/teacher/schedule`
- `GET /course/student/options`
- `GET /course/student/leave-options`
- `GET /course/teacher/options`
- `GET /course/{id}/students`（仅教师）
- `GET /course/attendance/teacher/list`（仅教师）
- `POST /attendance/check-in`（仅学生）
- `GET /attendance/records`（仅学生）

### 请假
- `POST /attendance/leave`（仅学生）
- `GET /attendance/leave/list`（仅学生）
- `PUT /attendance/leave/{id}/cancel`（仅学生）
- `GET /attendance/leave/pending`（仅教师）
- `PUT /attendance/leave/{id}/approve`（仅教师）

### 实验室预约
- `GET /lab/list`
- `GET /lab/{id}`
- `GET /lab/{id}/slots`
- `POST /lab/booking`（学生、教师）
- `GET /lab/booking/my`（学生、教师）
- `PUT /lab/booking/{id}/cancel`（学生、教师）
- `PUT /lab/booking/{id}/check-in`（学生、教师）
- `PUT /lab/booking/{id}/check-out`（学生、教师）

### 实验室管理员
- `GET /lab/booking/approve/list`
- `PUT /lab/booking/{id}/approve`
- `GET /lab/usage/stats`
- `GET /lab/manage/list`
- `POST /lab/manage`
- `PUT /lab/manage`
- `DELETE /lab/manage/{id}`
- `GET /dashboard/stats`

