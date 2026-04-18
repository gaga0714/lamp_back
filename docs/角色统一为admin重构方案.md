# 角色统一为 admin 重构方案

## 1. 重构目标

当前系统中存在多种角色：

- `student`
- `teacher`
- `labAdmin`
- `admin`

本次重构目标是：

将当前系统中的实验室管理权限和实验室预约审批权限统一收口到 `admin` 角色，不再单独使用 `labAdmin` 角色。

根据当前项目实际情况，推荐采用以下方案：

## 推荐方案

保留：

- `student`
- `teacher`
- `admin`

移除：

- `labAdmin`

原因：

1. 学生和教师仍然有明确业务意义
2. `admin` 统一承担实验室管理员职责
3. 改动量适中，逻辑清晰
4. 不影响登录、预约、考勤等既有流程

## 2. 当前项目中角色的使用位置

### 2.1 前端路由中的角色限制

当前前端路由定义如下：

```js
// 考勤管理
meta: { title: '考勤管理', icon: 'Setting', roles: ['teacher', 'admin'] }

// 预约审批
meta: { title: '预约审批', icon: 'CircleCheck', roles: ['labAdmin', 'admin'] }

// 实验室管理
meta: { title: '实验室管理', icon: 'Setting', roles: ['labAdmin', 'admin'] }

// 用户管理
meta: { title: '用户管理', icon: 'UserFilled', roles: ['admin'] }

// 管理员实验室管理入口
meta: { title: '实验室管理', icon: 'OfficeBuilding', roles: ['admin'] }
```

可以看出：

- `labAdmin` 只用于：
  - 预约审批
  - 实验室管理
- `admin` 已经具备这些权限
- `teacher` 目前主要用于考勤管理

### 2.2 前端菜单过滤逻辑

当前前端菜单权限过滤依据是：

```vue
const roles = r.meta?.roles
if (roles?.length && !roles.includes(role)) return false
```

也就是说：

- 当前登录用户的 `role`
- 必须命中对应路由 `meta.roles`
- 才能看到菜单和访问页面

### 2.3 后端真正做了权限控制的地方

后端有明确角色校验的地方主要是用户管理：

```java
private void requireAdmin() {
    String role = UserContext.getRole();
    if (!"admin".equals(role)) {
        throw new BusinessException(403, "无权限");
    }
}
```

当前实验室预约审批接口和实验室管理接口，更多依赖前端路由做角色限制，后端没有完全做细粒度角色区分。

这意味着当前统一为 `admin` 时：

- 前端改动更明显
- 后端角色改动相对少一些

### 2.4 注册页中的角色选择

当前注册页只允许用户选择：

- `student`
- `teacher`

说明 `labAdmin` 本来就不是开放注册角色，而是后台管理或初始化数据中使用的角色。

### 2.5 用户管理页中的角色选项

当前用户管理页中角色筛选包括：

- 研究生 `student`
- 教师 `teacher`
- 实验室管理员 `labAdmin`
- 系统管理员 `admin`

这里后续需要同步调整。

## 3. 重构范围说明

如果要把实验室业务都统一成 `admin`，重构范围如下：

### 必改

1. 前端路由中的 `labAdmin`
2. 前端用户管理页面中的角色文案和筛选
3. 数据库中已有 `labAdmin` 用户的角色值
4. 文档中角色说明

### 选改

1. 后端增加更明确的实验室审批和实验室管理接口权限控制
2. 注册逻辑和角色说明进一步简化
3. 测试数据中 `labAdmin` 改为 `admin`

## 4. 方案 A：仅移除 labAdmin，统一到 admin

保留角色：

- `student`
- `teacher`
- `admin`

删除角色：

- `labAdmin`

### 适用场景

- 学生仍需预约实验室
- 教师仍可能参与考勤管理
- 实验室审批和实验室管理统一由系统管理员完成

### 优点

- 改动量适中
- 业务语义合理
- 不会破坏现有学生/教师功能
- 最符合当前项目现状

## 5. 前端需要改什么

### 5.1 路由权限配置

当前：

```js
roles: ['labAdmin', 'admin']
```

重构后改为：

```js
roles: ['admin']
```

涉及页面：

1. `lab/approve`
2. `lab/manage`

文件：

`D:\bishe\lamp_front\src\router\routes.js`

重构后建议：

- 预约审批：只允许 `admin`
- 实验室管理：只允许 `admin`

如果未来希望教师也参与实验室审批，可扩展为：

```js
roles: ['teacher', 'admin']
```

### 5.2 用户管理页角色筛选

当前用户管理页里有：

- 实验室管理员 `labAdmin`

重构后应删除这一项。

文件：

`D:\bishe\lamp_front\src\views\admin\UsersView.vue`

需要改的地方：

#### 角色下拉选项

删除：

- `实验室管理员 / labAdmin`

#### 角色显示映射

删除：

- `labAdmin: '实验室管理员'`

重构后角色映射建议为：

- `student: 研究生`
- `teacher: 教师`
- `admin: 系统管理员`

### 5.3 其他前端角色文案清理

全局搜索这些关键字：

- `labAdmin`
- `实验室管理员`

需要检查：

- 页面文案
- 角色显示映射
- 用户管理筛选
- 文档说明页（如果前端有）

## 6. 后端需要改什么

### 6.1 User 实体角色注释更新

当前注释：

```java
/** student, teacher, labAdmin, admin */
private String role;
```

建议改为：

```java
/** student, teacher, admin */
private String role;
```

文件：

`d:\bishe\lamp_back\src\main\java\com\lamp\entity\User.java`

### 6.2 用户角色相关文档和 DTO 注释更新

当前 `RegisterDTO` 中角色注释为：

```java
private String role; // student, teacher
```

这里本身没有 `labAdmin`，问题不大，但建议整体保持一致说明。

### 6.3 AdminUserController 保持不变

当前 `AdminUserController` 已经只允许 `admin`：

```java
if (!"admin".equals(role)) {
    throw new BusinessException(403, "无权限");
}
```

这一点本来就是重构后的目标，因此不用改权限判断逻辑。

### 6.4 实验室相关接口建议补上 admin 权限校验

虽然前端路由已经限制了 `lab/approve` 和 `lab/manage`，但从后端安全角度，建议你在这些接口也补一层 `admin` 校验。

建议补权限控制的接口：

1. `GET /lab/booking/approve/list`
2. `PUT /lab/booking/{id}/approve`
3. `GET /lab/manage/list`
4. `POST /lab/manage`
5. `PUT /lab/manage`
6. `DELETE /lab/manage/{id}`

原因：

现在这些接口更多是前端页面限制，后端没有完全限制。
如果不补，非 `admin` 用户理论上直接调接口仍可能访问成功。

## 7. 数据库需要改什么

### 7.1 用户表中已有 labAdmin 数据改为 admin

如果数据库里已经有 `labAdmin` 角色的用户，直接执行：

```sql
UPDATE sys_user
SET role = 'admin'
WHERE role = 'labAdmin';
```

这样原先实验室管理员就统一为系统管理员。

### 7.2 检查是否还存在 labAdmin

执行：

```sql
SELECT id, username, name, role
FROM sys_user
WHERE role = 'labAdmin';
```

如果结果为空，说明数据库层已经完成清理。

## 8. 测试数据需要改什么

如果之前插入了这样的用户：

```sql
role = 'labAdmin'
```

例如：

- `LAB2025001`
- `实验室管理员`

建议改成：

```sql
role = 'admin'
```

否则会出现：

- 前端菜单不显示
- 后端角色判断不一致
- 测试角色混乱

## 9. 文档需要改什么

当前项目文档中还保留了 `labAdmin` 的角色说明，建议同步更新：

### 需要改的文档

1. `docs/数据库设计说明.md`
2. `docs/database.sql`
3. `README.md`
4. `docs/实验室模块优化设计.md`（如果其中提到了 labAdmin）

### 文档修改方向

把：

- `student`
- `teacher`
- `labAdmin`
- `admin`

统一改为：

- `student`
- `teacher`
- `admin`

## 10. 推荐实施步骤

### 第一步：数据库

执行：

```sql
UPDATE sys_user
SET role = 'admin'
WHERE role = 'labAdmin';
```

### 第二步：前端

修改：

- `src/router/routes.js`
- `src/views/admin/UsersView.vue`

### 第三步：后端

修改：

- `entity/User.java` 注释
- `README` 和文档说明
- 为实验室审批和实验室管理接口补 `admin` 权限校验

### 第四步：测试

验证这些账号：

1. `admin` 登录
   - 能看到预约审批
   - 能看到实验室管理
   - 能看到用户管理

2. 原 `labAdmin` 用户（已改成 `admin`）
   - 登录后应具备 `admin` 权限

3. `student`
   - 不应看到预约审批与实验室管理

4. `teacher`
   - 若保留考勤管理，应仍可看到考勤管理
   - 不应看到实验室审批（若统一成 `admin`）

## 11. 重构后的角色建议

重构完成后建议系统角色定义为：

### `student`

- 实验室查询
- 提交预约
- 查看我的预约
- 考勤签到
- 请假申请

### `teacher`

- 考勤管理
- 查看相关记录
- 不负责实验室审批

### `admin`

- 用户管理
- 实验室管理
- 预约审批
- 其他系统管理功能

## 12. 最终结论

如果目标是：

不再单独区分实验室管理员，实验室审批和实验室管理都统一交给系统管理员处理，

那么最合理的重构方案是：

- 删除 `labAdmin` 角色
- 保留 `student / teacher / admin`
- 所有原本 `labAdmin` 的前后端权限统一改为 `admin`
- 数据库中已有 `labAdmin` 用户统一改成 `admin`
- 实验室审批和实验室管理接口后端补上 `admin` 校验

这是对当前项目改动最小、逻辑最清晰、也最适合毕设答辩解释的一种方案。
