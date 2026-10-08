# 加利顿大学 · 基于 SpringBoot 的校园公告管理系统

> 加利顿大学校园信息中枢：把公告从「发出去」管到「看得见」——组织化下发、责任化审批、可量化回执、数据化运营。

## 一、交付物一览

| 交付物 | 路径 | 说明 |
|---|---|---|
| 📄 设计文档 | `docs/设计文档.md` | 世界观设定、六角色权限矩阵、12 模块 68 功能点、状态机、19 张表设计、接口清单、6 阶段制作步骤、12 条验收清单 |
| 🖥️ HTML 原型 | `prototype/campus-pulse.html` | **单文件、零依赖、离线可用**，双击即可打开。17 个页面视图 + 完整交互（登录换角色、审批流转、催办、编辑器、敏感词检测、Canvas 图表、深色模式） |
| ⚙️ 后端工程 | `backend/` | 可编译的 SpringBoot 3.3.5 + JDK 21 工程（140 个文件），含建表脚本与演示数据 |
| 🧪 校验脚本 | `tools/` | 原型静态一致性校验 + jsdom 无头冒烟测试 |

## 二、5 分钟跑起来

### 1. 先看原型（无需任何环境）

双击 `prototype/campus-pulse.html`，选择任一演示身份一键登录：

| 演示身份 | 能看到的差异 |
|---|---|
| 系统管理员 | 全部 16 个菜单，含用户权限、内容安全、操作审计、系统设置 |
| 校级审核员 | 审批中心出现待办，可审核 / 驳回 / 撤回 / 置顶 |
| 院系发布员 | 只能起草与发布本院公告，列表可编辑/提交 |
| 教职工 / 学生 | 无管理菜单，只有阅读、评论、回执 |
| 访客 | 仅公开公告只读 |

试试这几条主链路：**新建公告**（把分类切成「活动预告」会提示免审直发）→ **提交流程**（标题少于 6 字、正文少于 30 字、命中「包过」等阻断词都会被拦）→ **审批中心**（点驳回必须填 ≥5 字意见）→ **阅读回执**（一键催办）→ **内容安全**（在线检测实时高亮敏感词）。

### 2. 建库建表

```bash
mysql -uroot -p123456 < backend/src/main/resources/db/schema.sql
mysql -uroot -p123456 < backend/src/main/resources/db/data.sql
# 校验：六个状态都要有数据
mysql -uroot -p123456 -e "use campus_announcement; select status,count(*) from ann_announcement group by status;"
```

### 3. 启动后端

```bash
cd backend
mvn clean package -DskipTests
java -jar target/campus-announcement.jar
```

- 接口文档（Knife4j）：<http://localhost:8080/doc.html>
- 试一下登录：`POST /api/auth/login {"username":"admin","password":"123456"}` → 拿到 token
- 带 token 查公告：`GET /api/announcement/page?pageNum=1&pageSize=10`
- Redis 未启动时可先注释 `spring-boot-starter-data-redis` 相关配置，登录态改用本地缓存

### 4. 演示账号

| 账号 | 密码 | 角色 |
|---|---|---|
| `admin` | `123456` | 超级管理员 |
| `auditor` | `123456` | 校级审核员 |
| `publisher` | `123456` | 院系发布员（计算机学院） |
| `teacher` | `123456` | 教职工 |
| `student` | `123456` | 学生 |

## 三、后端结构

```
backend/
├── CONTRACT.md                    开发契约（类名/字段/接口冻结表，改前端前先读它）
├── pom.xml                        SpringBoot 3.3.5 · JDK 21 · MyBatis-Plus · Druid · JWT · Knife4j
└── src/main/
    ├── java/com/ivy/campus/
    │   ├── CampusApplication.java  @EnableScheduling @EnableAsync @MapperScan
    │   ├── common/                 R / PageResult / ResultCode / BizException / 全局异常 / 常量
    │   ├── config/                 MyBatis-Plus 分页与自动填充 / Web(CORS+拦截器+静态资源) / Redis / Knife4j / 异步线程池
    │   ├── security/               JwtUtil / LoginUser / UserContext / AuthInterceptor / @RequireRole
    │   ├── util/                   SecurityUtils / PageUtils / IpUtils / RedisUtil / SensitiveWordUtil
    │   ├── entity/                 19 个实体（对应 19 张表）
    │   ├── mapper/                 19 个 Mapper + 自定义 SQL
    │   ├── dto/ vo/                入参与出参模型
    │   ├── service/ impl/          15 个业务服务（审批流 / 回执 / 推送 / 统计 / 文件）
    │   ├── controller/             13 个 REST 控制器
    │   └── aspect/                 操作审计切面（AOP 自动写 sys_operation_log）
    └── resources/
        ├── application.yml         端口 8080 / Druid / MyBatis-Plus / Redis / 上传目录
        ├── db/schema.sql           19 张表 + 索引 + 唯一键
        ├── db/data.sql             组织树 / 分类 / 角色 / 8 用户 / 12 条多状态公告 / 回执 / 评论
        └── mapper/*.xml            分页查询、分类统计、趋势、部门未读排行、日志分页
```

## 四、核心业务怎么实现的

**公告状态机**（`AnnouncementServiceImpl`）
```
DRAFT --submit--> PENDING --audit(通过)--> PUBLISHED / SCHEDULED --定时任务--> PUBLISHED
                       \--audit(驳回)--> REJECTED --编辑后重新 submit--> PENDING
PUBLISHED --withdraw--> WITHDRAWN     PUBLISHED --expireTime 到期--> ARCHIVED
分类 needAudit=0（活动预告）时 submit 直接发布，审批日志备注「分类免审自动通过」
```

**发布时发生了什么**：按可见范围（全校 / 本学院 / 指定班级 / 自定义）计算目标用户 → 批量写入 `ann_receipt`（每用户一行）→ 回填 `targetCount` → 异步写 `ann_push_log`（站内信/邮件/短信）→ 给目标用户写 `sys_notice` → 清 Redis 统计缓存。

**数据范围三层叠加**：角色（RBAC）× 数据范围（ALL/COLLEGE/SELF）× 公告可见范围。学生看不到任何非已发布公告，院系发布员看不到别的学院草稿。

**审计与留痕**：每次状态流转写 `ann_approval_log`（节点/操作人/意见/前后状态），每个 Controller 方法由 AOP 写 `sys_operation_log`（IP / 耗时 / 结果）。

## 五、把原型变成真前端

原型里的数据全在 `prototype/campus-pulse.html` 顶部的 `Mock 数据` 区（`ANNS / CATEGORIES / USERS / DEPTS …`），接口调用点集中在 `ACTIONS` 对象里。三种落地方式：

1. **Vue3 + Element Plus（推荐毕设）**：按原型页面拆分组件，图表换 ECharts，`ACTIONS.xxx` 换成 `axios.post('/api/...')`。
2. **保留单文件原型**：把 Mock 层替换为 `fetch`，token 存 `localStorage`，即可当真实前端用。
3. **Thymeleaf 直出**：模板复用 `redhead` 红头文件结构，最省事但交互弱。

## 六、自检脚本

```bash
cd tools
node check-prototype.js ../prototype/campus-pulse.html   # JS 语法 / data-act 与 ACTIONS 对齐 / DOM id 引用 / 标签配对
npm install jsdom && node smoke-prototype.js ../prototype/campus-pulse.html   # 无头跑通登录、17 视图、审批、催办、编辑器
```

冒烟测试覆盖：登录 → 16 个菜单逐个渲染 → 详情抽屉（红头文件/审批时间线）→ 催办 → 审核通过与驳回（含意见必填与 ≥5 字校验）→ 评论审核与官方回复 → 内容安全检测 → 深色模式 → 编辑器的空表单拦截 / 免审直发 / 草稿保存 / 红头预览 → 列表筛选与分页 → 6 种角色切换。

## 七、常见问题

| 现象 | 处理 |
|---|---|
| 后端启动报数据库连接失败 | 确认 MySQL 已建库并改 `application.yml` 的账号密码 |
| 登录后接口 401 | 请求头要带 `Authorization: Bearer <token>` |
| 上传附件失败 | 附件的目录默认是「运行目录下的 `upload/`」，可写即可；若想固定位置，把 `application.yml` 的 `campus.upload.path` 改成绝对路径 |
| 定时发布不生效 | 确认启动类有 `@EnableScheduling`，且公告状态为 `SCHEDULED` 且 `publish_time <= now()` |
| 原型打不开图表 | 用 Chrome/Edge 打开；无 canvas 环境会自动跳过绘制，不影响其他功能 |
