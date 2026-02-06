# 研究生考勤及实验室预约系统 - 后端

基于 Spring Boot 2 + JPA + MySQL 的后端服务，与前端 [lamp_front](../lamp_front) 前后端分离对接。

## 技术栈

- Java 8 (JDK 1.8)
- Spring Boot 2.7
- Spring Data JPA
- MySQL 8 / H2（可选）
- JWT 认证（jjwt 0.11）
- Lombok

## 接口前缀与跨域

- 上下文路径：`/api`，所有接口形如 `http://localhost:8080/api/auth/login`
- 前端开发环境通过 Vite 代理将 `/api` 转发到 `http://localhost:8080`，因此前端请求 `baseURL: '/api'` 时实际访问 `http://localhost:8080/api/...`
- 已配置 CORS，允许前端跨域访问

## 响应格式

统一包装为：

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

成功时 `code` 为 200，前端会取 `data` 使用；失败时 `code` 非 200，`message` 为错误信息。

## 认证

- 除 `POST /api/auth/login`、`POST /api/auth/register` 外，其余接口需在请求头携带：`Authorization: Bearer <token>`
- 未登录或 token 无效时返回 401

## 默认管理员

首次启动会自动创建管理员账号（若不存在）：

- 用户名：`admin`
- 密码：`admin123`
- 角色：`admin`

## 运行方式

### 使用 MySQL

1. **建库建表**：执行项目中的建表脚本（推荐）：
   ```bash
   mysql -u root -p < docs/database.sql
   ```
   或手动创建数据库后，在 MySQL 客户端中执行 `docs/database.sql` 内的建表语句。  
   数据库设计说明见 [docs/数据库设计说明.md](docs/数据库设计说明.md)。

   若希望由 JPA 自动建表，可跳过建表脚本，在配置中保留 `createDatabaseIfNotExist=true`，JPA 会按实体创建/更新表。

2. 修改 `src/main/resources/application.yml` 中的数据库连接：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/lamp?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&createDatabaseIfNotExist=true
    username: root
    password: 你的密码
```

3. 启动项目：

```bash
mvn spring-boot:run
```

服务地址：`http://localhost:8080`，接口基础路径：`http://localhost:8080/api`。

### 使用 H2（无需安装 MySQL）

在 `application.yml` 中注释掉 MySQL 配置，启用 H2 配置（文件中已有注释示例），然后执行：

```bash
mvn spring-boot:run
```

## 主要接口一览

| 模块     | 方法 | 路径 | 说明 |
|----------|------|------|------|
| 认证     | POST | /auth/login | 登录 |
| 认证     | POST | /auth/register | 注册 |
| 认证     | GET  | /auth/user | 当前用户信息 |
| 认证     | PUT  | /auth/password | 修改密码 |
| 用户     | PUT  | /user/profile | 更新个人资料 |
| 用户管理 | GET  | /admin/users | 用户列表（管理员） |
| 用户管理 | POST/PUT/DELETE | /admin/users, /admin/users/:id | 增删改用户 |
| 考勤     | POST | /attendance/check-in | 签到/签退 |
| 考勤     | GET  | /attendance/records | 我的考勤记录 |
| 考勤     | GET  | /attendance/manage | 考勤管理列表（教师/管理员） |
| 请假     | POST | /attendance/leave | 请假申请 |
| 请假     | GET  | /attendance/leave/list | 我的请假列表 |
| 请假     | PUT  | /attendance/leave/:id/approve | 审批请假 |
| 实验室   | GET  | /lab/list | 实验室列表 |
| 实验室   | GET  | /lab/:id | 实验室详情 |
| 实验室   | GET  | /lab/:id/slots | 可预约时段 |
| 预约     | POST | /lab/booking | 提交预约 |
| 预约     | GET  | /lab/booking/my | 我的预约 |
| 预约     | PUT  | /lab/booking/:id/cancel | 取消预约 |
| 预约审批 | GET  | /lab/booking/approve/list | 待审批预约列表 |
| 预约审批 | PUT  | /lab/booking/:id/approve | 审批预约 |
| 实验室管理 | GET  | /lab/manage/list | 实验室管理列表 |
| 实验室管理 | POST/PUT/DELETE | /lab/manage, /lab/manage/:id | 实验室增删改 |

角色说明：`student` 研究生、`teacher` 教师、`labAdmin` 实验室管理员、`admin` 系统管理员。考勤管理需教师或管理员；预约审批与实验室管理需实验室管理员或系统管理员。
