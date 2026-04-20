# 角色统一为 admin 重构方案

## 1. 文档说明

本文件保留“将 `labAdmin` 统一到 `admin`”的设计背景，但内容已按当前项目状态整理。

当前项目已经完成以下收敛：
- 不再使用 `labAdmin`
- 系统角色统一为 `student / teacher / admin`
- `admin` 的业务语义已固定为“实验室管理员”

## 2. 当前角色定义

### `student`
- 我的课表
- 课程签到
- 课程考勤
- 课程请假 / 我的请假
- 实验室列表 / 我的预约

### `teacher`
- 我的授课
- 课程考勤管理
- 请假审批
- 实验室列表 / 我的预约

### `admin`
- 实验室列表
- 预约审批
- 实验室管理
- 实验室使用统计
- 个人资料 / 修改密码

## 3. 已完成的统一结果

### 3.1 角色值统一

数据库、代码、文档当前都应只保留：
- `student`
- `teacher`
- `admin`

### 3.2 `admin` 语义更新

这里的 `admin` 不再表示“系统管理员”，而表示“实验室管理员”。

因此当前 `admin` 不再承担：
- 用户管理
- 课表管理
- 考勤总览
- 课程学生名单查看

### 3.3 注册与登录

当前注册页仍只开放：
- `student`
- `teacher`

`admin` 账号仍通过初始化或数据库维护方式产生，不通过前台注册。

## 4. 当前前端现状

当前前端路由中的管理员页面已经收敛为：
- `lab/approve`
- `lab/usage-stats`
- `lab/manage`

已不再对管理员暴露：
- `course/manage`
- `attendance/manage`
- `admin/users`

## 5. 当前后端现状

当前后端中：
- 实验室相关后台接口仍由 `admin` 访问
- 非实验室的课程管理、考勤总览、用户管理接口已对 `admin` 收口

也就是说，`admin` 虽然保留了这个角色值，但权限边界已经不再是“全局后台管理员”。

## 6. 数据层说明

如果历史数据中仍存在旧角色值 `labAdmin`，应统一改为：

```sql
UPDATE sys_user
SET role = 'admin'
WHERE role = 'labAdmin';
```

检查语句：

```sql
SELECT id, username, name, role
FROM sys_user
WHERE role = 'labAdmin';
```

结果应为空。

## 7. 当前文档基准

当前项目文档统一应以以下表述为准：

- `student`：研究生
- `teacher`：教师
- `admin`：实验室管理员

不再使用：
- `labAdmin`
- “admin=系统管理员”的旧说法

## 8. 当前结论

“角色统一为 admin”这一步已经不是未来计划，而是当前已落地现状：

- 角色值只保留 `student / teacher / admin`
- `labAdmin` 已退出当前系统定义
- `admin` 的最终语义是实验室管理员，而不是全功能系统管理员
