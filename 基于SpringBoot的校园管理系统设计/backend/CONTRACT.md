# 加利顿大学 · 后端开发契约（所有子任务必须严格遵守）

> 项目根：`C:\Users\Administrator\Desktop\基于SpringBoot的校园管理系统设计\backend`
> 本文件是唯一事实来源。命名、字段、方法签名一律照抄，禁止自创。

## 1. 技术栈与版本（禁止改动）

- JDK 21，Spring Boot 3.3.5，Maven 构建
- groupId `com.ivy`，artifactId `campus-announcement`，version `1.0.0`，jar 打包
- 基础包名 `com.ivy.campus`
- MyBatis-Plus 3.5.9：`com.baomidou:mybatis-plus-spring-boot3-starter:3.5.9`
- MySQL：`com.mysql:mysql-connector-j`（runtime）
- Redis：`spring-boot-starter-data-redis`
- 安全：`io.jsonwebtoken:jjwt-api/jjwt-impl/jjwt-jackson:0.12.6`（不用 Spring Security，用拦截器 + JWT）
- 接口文档：`com.github.xiaoymin:knife4j-openapi3-jakarta-spring-boot-starter:4.5.0`
- 工具：`cn.hutool:hutool-all:5.8.32`、`org.projectlombok:lombok`、`spring-boot-starter-validation`、`spring-boot-starter-mail`、`com.alibaba:druid-spring-boot-3-starter:1.2.23`
- 端口 8080；数据库 `campus_announcement`（root/123456）；Redis `localhost:6379` db1；文件上传目录由 `campus.upload.path` 配置（默认相对路径 `./upload/`），映射 `/upload/**`

## 2. 统一响应与通用类（A 组产出，C 组直接调用）

```java
// com.ivy.campus.common.R
public class R<T> {            // 字段：code(Integer) message(String) data(T) timestamp(Long)
  public static <T> R<T> ok();                       // code=200 message="操作成功"
  public static <T> R<T> ok(T data);
  public static <T> R<T> ok(String message, T data);
  public static <T> R<T> fail(String message);       // code=500
  public static <T> R<T> fail(Integer code, String message);
  public boolean isSuccess();
}
// com.ivy.campus.common.PageResult<T>  字段：records(List<T>) total(Long) pageNum(Long) pageSize(Long) pages(Long)
//   public static <T> PageResult<T> of(IPage<T> page); public static <T> PageResult<T> of(List<T> records, long total, long pageNum, long pageSize);
// com.ivy.campus.common.ResultCode  枚举：SUCCESS(200,"操作成功") PARAM_ERROR(400,"参数校验失败") UNAUTHORIZED(401,"未登录或登录已过期") FORBIDDEN(403,"无操作权限") NOT_FOUND(404,"资源不存在") BIZ_ERROR(500,"业务处理失败")
// com.ivy.campus.common.BizException extends RuntimeException  构造：BizException(String message) / BizException(Integer code,String message) / BizException(ResultCode rc)  getter: getCode()
// com.ivy.campus.common.GlobalExceptionHandler  @RestControllerAdvice 处理 BizException / MethodArgumentNotValidException / Exception
// com.ivy.campus.common.Constants  String TOKEN_HEADER="Authorization"; String TOKEN_PREFIX="Bearer "; String REDIS_TOKEN_PREFIX="campus:token:"; String REDIS_STATS_PREFIX="campus:stats:";
```

## 3. 登录态与鉴权（A 组产出签名，C 组调用）

```java
// com.ivy.campus.security.JwtUtil
String generateToken(Long userId, String username, String roleCode);
Long getUserId(String token);
String getUsername(String token);
String getRoleCode(String token);
boolean validate(String token);

// com.ivy.campus.security.LoginUser  字段：Long userId; String username; String realName; String roleCode; Long deptId; String dataScope; Integer userType;
// com.ivy.campus.security.UserContext  静态方法：set(LoginUser) get() LoginUser getUserId() Long getRoleCode() String getDeptId() Long clear()
// com.ivy.campus.security.AuthInterceptor  拦截 /api/**，放行 /api/auth/login、/api/auth/register、/api/auth/captcha、/doc.html、/webjars/**、/v3/api-docs/**、/upload/**
// com.ivy.campus.security.RequireRole  @interface，value() 为 String[] 允许的角色码；由 AuthInterceptor 校验方法/类级注解
// com.ivy.campus.util.SecurityUtils  static Long userId(); static String roleCode(); static Long deptId(); static boolean hasRole(String... roles); static boolean isAdmin();
// com.ivy.campus.util.PageUtils  static <T> Page<T> of(Integer pageNum, Integer pageSize);  // 默认 1 / 10，pageSize 上限 100
```

## 4. 实体清单（B 组产出，字段名严格照抄）

所有实体：`@Data @TableName("表名")`，主键 `@TableId(type = IdType.AUTO) private Long id;`，逻辑删除字段 `@TableLogic private Integer deleted;`（若表有），时间字段 `@JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")`。表名与类名映射如下（表名 = 下划线复数）：

| 类名 | 表名 | 字段（除 id 外，按顺序） |
|---|---|---|
| User | sys_user | username, password, realName, nickname, avatar, email, phone, gender(Integer 0未知1男2女), deptId, studentNo, userType(String STUDENT/TEACHER/STAFF), roleCode, status(Integer 1正常0停用), lastLoginTime(LocalDateTime), createTime, updateTime, deleted |
| Dept | sys_dept | parentId, name, code, type(SCHOOL/COLLEGE/DEPT/CLASS), campus, sort, leaderName, status, createTime, updateTime, deleted |
| Role | sys_role | code, name, description, dataScope(ALL/COLLEGE/SELF), sort, status, createTime |
| Category | ann_category | name, code, color, icon, needAudit(Integer 1需要审核), sort, status, description, createTime, updateTime, deleted |
| Announcement | ann_announcement | title, subtitle, summary, content, coverImage, categoryId, authorId, deptId, campus, status, visibility(ALL/COLLEGE/CLASS/CUSTOM), priority(NORMAL/IMPORTANT/URGENT), isTop(Integer), isBanner(Integer), isRedHead(Integer), allowComment(Integer), needReceipt(Integer), publishTime, expireTime, auditUserId, auditTime, auditRemark, viewCount(Integer), receiptCount(Integer), targetCount(Integer), version(Integer), keywords, createBy, createTime, updateTime, deleted |
| AnnouncementAttachment | ann_attachment | announcementId, fileName, filePath, fileSize(Long), fileType, downloadCount(Integer), createTime |
| AnnouncementScope | ann_scope | announcementId, scopeType(String COLLEGE/CLASS/USER), scopeValue(Long), scopeName, createTime |
| Tag | ann_tag | name, color, useCount(Integer), createTime |
| AnnouncementTagRel | ann_tag_rel | announcementId, tagId |
| AnnouncementVersion | ann_version | announcementId, versionNo(Integer), title, content, editorId, editorName, changeLog, createTime |
| Comment | ann_comment | announcementId, userId, parentId, content, likeCount(Integer), status(PENDING/APPROVED/REJECTED), auditUserId, createTime, deleted |
| ApprovalLog | ann_approval_log | announcementId, node(SUBMIT/DEPT_AUDIT/UNIV_AUDIT/PUBLISH/REJECT/WITHDRAW/ARCHIVE/UPDATE), operatorId, operatorName, action, remark, fromStatus, toStatus, createTime |
| Receipt | ann_receipt | announcementId, userId, deptId, readFlag(Integer), readTime, readDuration(Integer), urgeCount(Integer), lastUrgeTime, createTime |
| Subscription | ann_subscription | userId, categoryId, channel(SITE/EMAIL/SMS/WECHAT/DINGTALK), enabled(Integer), createTime |
| PushLog | ann_push_log | announcementId, channel, targetCount(Integer), successCount(Integer), failCount(Integer), status(Integer), errorMsg, createTime |
| Notice | sys_notice | userId, title, content, type(SYSTEM/AUDIT/ANNOUNCE/URGE), bizId, readFlag(Integer), createTime |
| OperationLog | sys_operation_log | userId, username, module, operation, method, params, ip, location, costTime(Long), status(Integer), errorMsg, createTime |
| SensitiveWord | sys_sensitive_word | word, level(BLOCK/WARN), replacement, status, createTime |
| Template | ann_template | name, categoryId, title, content, useCount(Integer), creatorId, status, createTime |

## 5. Mapper 契约（B 组产出接口 + XML，C 组只能调用这些方法）

每个实体一个 `com.ivy.campus.mapper.XxxMapper extends BaseMapper<Xxx>`。另必须提供以下自定义方法（XML 放 `src/main/resources/mapper/`）：

```java
// AnnouncementMapper（额外自定义）
IPage<Announcement> selectAnnouncementPage(IPage<Announcement> page, @Param("q") AnnouncementQuery query);
List<CategoryStatVO> selectCategoryStats(@Param("days") Integer days);
List<TrendVO> selectPublishTrend(@Param("days") Integer days);
List<Announcement> selectTopViewed(@Param("limit") Integer limit);
Long countByStatus(@Param("status") String status);          // 全部状态
// ReceiptMapper
List<DeptUnreadVO> selectUnreadRank(@Param("announcementId") Long announcementId);
Long countUnreadByAnnouncement(@Param("announcementId") Long announcementId);
// OperationLogMapper
IPage<OperationLog> selectLogPage(IPage<OperationLog> page, @Param("q") LogQuery query);
```

`AnnouncementQuery`（DTO）：keyword, categoryId, status, visibility, priority, campus, deptId, authorId, onlyTop, startTime, endTime, pageNum, pageSize。
`LogQuery`（DTO）：username, module, operation, status, startTime, endTime, pageNum, pageSize。

## 6. Service 接口签名（C 组产出，控制器只调这些）

```java
// AuthService
LoginVO login(LoginDTO dto);                 // 校验密码(MD5+盐, hutool DigestUtil)，签发 token，返回 token+UserVO，更新 lastLoginTime
void register(RegisterDTO dto);
Map<String,String> captcha();                // 返回 {"uuid":..,"code":..}（演示用，真实项目走图形验证码）

// AnnouncementService
PageResult<AnnouncementVO> page(AnnouncementQuery query);
AnnouncementDetailVO detail(Long id, boolean countView);
Long saveDraft(AnnouncementDTO dto);
void update(Long id, AnnouncementDTO dto);
void submit(Long id);                        // DRAFT/REJECTED -> PENDING，写审批日志 + 通知审核员
void audit(Long id, AuditDTO dto);           // PENDING -> PUBLISHED/SCHEDULED/REJECTED
void publish(Long id);
void withdraw(Long id);
void archive(Long id);
void toggleTop(Long id);
void toggleBanner(Long id);
void remove(Long id);                        // 逻辑删除
List<AnnouncementVO> latest(Integer limit);
List<AnnouncementVO> banner();

// ApprovalService
List<ApprovalLogVO> timeline(Long announcementId);
PageResult<AnnouncementVO> todoList(Integer pageNum, Integer pageSize);

// CommentService
PageResult<CommentVO> pageByAnnouncement(Long announcementId, Integer pageNum, Integer pageSize);
Long add(CommentDTO dto);
void audit(Long id, String status);
void remove(Long id);

// ReceiptService
void markRead(Long announcementId);
ReceiptVO statByAnnouncement(Long announcementId);
List<DeptUnreadVO> unreadRank(Long announcementId);
Long urge(Long announcementId, Long deptId);   // 催办，写 Notice + urgeCount+1

// StatsService
StatsOverviewVO overview();
List<TrendVO> trend(Integer days);
List<CategoryStatVO> categoryStats(Integer days);
List<DeptUnreadVO> deptUnread();
List<AnnouncementVO> topViewed(Integer limit);

// NoticeService
PageResult<NoticeVO> myNotices(Integer pageNum, Integer pageSize, Integer readFlag);
Long unreadCount();
void read(Long id);
void readAll();

// UserService / DeptService / CategoryService / TemplateService / SensitiveWordService
// 均提供 page(...) / getById / save / update / remove / listAll 语义方法，签名自洽即可，命名遵循同风格。

// FileService
String upload(MultipartFile file);           // 返回可访问 URL，按日期分目录
void download(Long attachmentId, HttpServletResponse response);
```

## 7. Controller 契约（C 组产出）

统一前缀与允许角色：

| Controller | 前缀 | 主要接口 | 允许角色 |
|---|---|---|---|
| AuthController | /api/auth | POST /login、POST /register、GET /captcha、GET /info、POST /logout | 全部 |
| AnnouncementController | /api/announcement | GET /page、GET /{id}、POST /save、PUT /{id}、POST /{id}/submit、POST /{id}/publish、POST /{id}/withdraw、POST /{id}/archive、POST /{id}/top、POST /{id}/banner、DELETE /{id}、GET /latest、GET /banner | 查:全部；写:DEPT_PUBLISHER/UNIV_AUDITOR/SUPER_ADMIN |
| ApprovalController | /api/approval | POST /{id}/audit、GET /timeline/{id}、GET /todo | UNIV_AUDITOR/SUPER_ADMIN |
| CategoryController | /api/category | GET /list、GET /page、POST、PUT /{id}、DELETE /{id} | 写:SUPER_ADMIN |
| CommentController | /api/comment | GET /page、POST、POST /{id}/audit、DELETE /{id} | 全部/写:登录用户 |
| ReceiptController | /api/receipt | POST /read/{announcementId}、GET /stat/{announcementId}、GET /unread/{announcementId}、POST /urge | 登录用户，统计类 ADMIN/AUDITOR/PUBLISHER |
| NoticeController | /api/notice | GET /page、GET /unread、POST /{id}/read、POST /readAll | 登录用户 |
| StatsController | /api/stats | GET /overview、GET /trend、GET /category、GET /deptUnread、GET /topViewed | 登录用户 |
| UserController | /api/user | GET /page、GET /{id}、PUT /{id}、DELETE /{id}、POST /resetPwd | SUPER_ADMIN |
| DeptController | /api/dept | GET /tree、GET /list、POST、PUT /{id}、DELETE /{id} | 查:登录用户；写:SUPER_ADMIN |
| FileController | /api/file | POST /upload、GET /download/{attachmentId} | 登录用户 |
| TemplateController | /api/template | GET /page、POST、PUT /{id}、DELETE /{id} | 写:ADMIN/AUDITOR/PUBLISHER |
| SensitiveWordController | /api/sensitive | GET /list、GET /page、GET /{id}、POST、PUT /{id}、DELETE /{id}、POST /check | 词库维护:SUPER_ADMIN；内容检测:SUPER_ADMIN/UNIV_AUDITOR/DEPT_PUBLISHER |
| LogController | /api/log | GET /page | SUPER_ADMIN |

返回类型统一 `R<T>`；分页统一 `PageResult<T>`；控制器方法加 `@Tag/@Operation`（Knife4j）与 `@Validated`。

## 8. 业务状态机（AnnouncementService 必须实现）

```
DRAFT --submit--> PENDING --audit(通过,needAudit=1)--> PUBLISHED
                        \--audit(通过,未来 publishTime)--> SCHEDULED --定时任务--> PUBLISHED
                        \--audit(驳回)--> REJECTED --update/submit--> PENDING
DRAFT --submit, 分类 needAudit=0--> PUBLISHED（自动通过，日志记 auto）
PUBLISHED --withdraw--> WITHDRAWN ; PUBLISHED --expireTime 到期--> ARCHIVED（定时任务）
```
发布时：生成 Receipt 目标行（按 visibility 计算 targetCount）、写 PushLog（SITE/EMAIL/SMS）、创建 Notice、清空相关 Redis 统计缓存。
定时任务：`@Scheduled(cron = "0 */1 * * * ?")` 处理 SCHEDULED 到期发布与 expireTime 归档。

## 9. 代码规范

- 禁止使用 `System.out.println`，统一 `@Slf4j`。
- 所有 ServiceImpl 加 `@Service`、`@RequiredArgsConstructor`、`@Transactional(rollbackFor = Exception.class)`（写操作）。
- 分页用 `PageUtils.of(query.getPageNum(), query.getPageSize())` + MyBatis-Plus `IPage`。
- 时间统一 `LocalDateTime`，DTO 入参时间用 `@JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")`。
- 不引入 Spring Security；不写前端资源；不写单元测试。
- 每个文件顶部不加多余注释，方法上可用一行中文注释说明业务。

## 10. VO 字段冻结表（XML `resultType` 与 Service 必须完全一致）

```java
// com.ivy.campus.vo.CategoryStatVO   -> name(String) code(String) color(String) value(Long)
// com.ivy.campus.vo.TrendVO          -> date(String) count(Long) publishCount(Long) readRate(Double)
// com.ivy.campus.vo.DeptUnreadVO     -> deptId(Long) deptName(String) total(Long) unread(Long) rate(Double)
// com.ivy.campus.vo.AnnouncementVO   -> id title subtitle summary categoryId categoryName categoryColor
//    authorId authorName deptId deptName campus status statusText visibility priority isTop isBanner
//    isRedHead allowComment needReceipt publishTime expireTime viewCount receiptCount targetCount
//    commentCount createTime
// com.ivy.campus.vo.AnnouncementDetailVO extends AnnouncementVO -> content keywords auditRemark auditUserName
//    attachments(List<AnnouncementAttachment>) tags(List<Tag>) scopes(List<AnnouncementScope>)
//    approvalLogs(List<ApprovalLogVO>) receiptRate(Double)
// com.ivy.campus.vo.ApprovalLogVO    -> id announcementId node nodeText operatorId operatorName action remark fromStatus toStatus createTime
// com.ivy.campus.vo.CommentVO        -> id announcementId userId userName userAvatar deptName content likeCount status statusText parentId children(List<CommentVO>) createTime
// com.ivy.campus.vo.ReceiptVO        -> announcementId total readCount unreadCount rate(List<DeptUnreadVO>) deptRanks
// com.ivy.campus.vo.NoticeVO         -> id title content type bizId readFlag createTime
// com.ivy.campus.vo.StatsOverviewVO  -> totalAnnouncement todayPublish pendingAudit published publishedThisMonth totalUser todayActive totalRead unreadUrge commentPending topCategory
// com.ivy.campus.vo.LoginVO          -> token tokenType expiresIn(Long) user(UserVO)
// com.ivy.campus.vo.UserVO           -> id username realName nickname avatar email phone gender deptId deptName
//    studentNo userType roleCode roleName status lastLoginTime createTime
```

所有 VO 使用 `@Data`（`AnnouncementDetailVO` 用 `@Data @EqualsAndHashCode(callSuper = true)`），日期字段 `@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")`。
