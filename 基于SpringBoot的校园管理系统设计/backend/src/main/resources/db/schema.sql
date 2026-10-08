-- ============================================================
-- 加利顿大学 校园公告管理系统 · 表结构
-- 数据库：campus_announcement  字符集：utf8mb4
-- 说明：所有字段名与契约 §4 实体字段严格一一对应
--       逻辑删除统一使用 deleted（0 未删除 / 1 已删除）
--       外键关系以注释形式标注，实际由应用层保证一致性
-- ============================================================

CREATE DATABASE IF NOT EXISTS `campus_announcement`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE `campus_announcement`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ------------------------------------------------------------
-- 1. 校园组织结构表 sys_dept（SCHOOL 学校 / COLLEGE 学院 / DEPT 部门 / CLASS 班级）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `parent_id`   bigint       NULL DEFAULT 0 COMMENT '上级部门ID，根节点为0 外键-> sys_dept.id',
    `name`        varchar(100) NOT NULL COMMENT '部门名称',
    `code`        varchar(64)  NULL DEFAULT NULL COMMENT '部门编码',
    `type`        varchar(20)  NOT NULL DEFAULT 'DEPT' COMMENT '类型 SCHOOL/COLLEGE/DEPT/CLASS',
    `campus`      varchar(50)  NULL DEFAULT NULL COMMENT '所属校区',
    `sort`        int          NOT NULL DEFAULT 0 COMMENT '排序号',
    `leader_name` varchar(50)  NULL DEFAULT NULL COMMENT '负责人姓名',
    `status`      tinyint      NOT NULL DEFAULT 1 COMMENT '状态 1正常 0停用',
    `create_time` datetime     NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     tinyint      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
    PRIMARY KEY (`id`),
    KEY `idx_dept_parent` (`parent_id`),
    KEY `idx_dept_type` (`type`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='校园组织结构表';

-- ------------------------------------------------------------
-- 2. 角色表 sys_role  dataScope：ALL 全校 / COLLEGE 本院 / SELF 仅本人
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `code`        varchar(50)  NOT NULL COMMENT '角色码 SUPER_ADMIN/UNIV_AUDITOR/DEPT_PUBLISHER/TEACHER/STUDENT',
    `name`        varchar(50)  NOT NULL COMMENT '角色名称',
    `description` varchar(255) NULL DEFAULT NULL COMMENT '角色说明',
    `data_scope`  varchar(20)  NOT NULL DEFAULT 'SELF' COMMENT '数据范围 ALL/COLLEGE/SELF',
    `sort`        int          NOT NULL DEFAULT 0 COMMENT '排序号',
    `status`      tinyint      NOT NULL DEFAULT 1 COMMENT '状态 1正常 0停用',
    `create_time` datetime     NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`code`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='角色表';

-- ------------------------------------------------------------
-- 3. 用户表 sys_user  外键 -> sys_dept.id（dept_id）、sys_role.code（role_code）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`
(
    `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`        varchar(50)  NOT NULL COMMENT '登录名',
    `password`        varchar(64)  NOT NULL COMMENT '密码（MD5 32位大写）',
    `real_name`       varchar(50)  NULL DEFAULT NULL COMMENT '真实姓名',
    `nickname`        varchar(50)  NULL DEFAULT NULL COMMENT '昵称',
    `avatar`          varchar(255) NULL DEFAULT NULL COMMENT '头像地址',
    `email`           varchar(100) NULL DEFAULT NULL COMMENT '邮箱',
    `phone`           varchar(20)  NULL DEFAULT NULL COMMENT '手机号',
    `gender`          tinyint      NOT NULL DEFAULT 0 COMMENT '性别 0未知 1男 2女',
    `dept_id`         bigint       NULL DEFAULT NULL COMMENT '所属部门ID 外键-> sys_dept.id',
    `student_no`      varchar(30)  NULL DEFAULT NULL COMMENT '学号/工号',
    `user_type`       varchar(20)  NOT NULL DEFAULT 'STUDENT' COMMENT '用户类型 STUDENT/TEACHER/STAFF',
    `role_code`       varchar(50)  NOT NULL DEFAULT 'STUDENT' COMMENT '角色码 外键-> sys_role.code',
    `status`          tinyint      NOT NULL DEFAULT 1 COMMENT '状态 1正常 0停用',
    `last_login_time` datetime     NULL DEFAULT NULL COMMENT '最后登录时间',
    `create_time`     datetime     NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         tinyint      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_username` (`username`),
    KEY `idx_user_dept` (`dept_id`),
    KEY `idx_user_role` (`role_code`),
    KEY `idx_user_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户表';

-- ------------------------------------------------------------
-- 4. 公告分类表 ann_category  needAudit：1 需要审核 / 0 免审核自动发布
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `ann_category`;
CREATE TABLE `ann_category`
(
    `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`         varchar(50)  NOT NULL COMMENT '分类名称',
    `code`         varchar(50)  NOT NULL COMMENT '分类编码',
    `color`        varchar(20)  NULL DEFAULT NULL COMMENT '展示颜色',
    `icon`         varchar(50)  NULL DEFAULT NULL COMMENT '展示图标',
    `need_audit`   tinyint      NOT NULL DEFAULT 1 COMMENT '是否需要审核 1需要 0不需要',
    `sort`         int          NOT NULL DEFAULT 0 COMMENT '排序号',
    `status`       tinyint      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    `description`  varchar(255) NULL DEFAULT NULL COMMENT '分类说明',
    `create_time`  datetime     NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  datetime     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`      tinyint      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_category_code` (`code`),
    KEY `idx_category_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='公告分类表';

-- ------------------------------------------------------------
-- 5. 公告主表 ann_announcement
--    status     DRAFT/PENDING/REJECTED/SCHEDULED/PUBLISHED/WITHDRAWN/ARCHIVED
--    visibility ALL/COLLEGE/CLASS/CUSTOM   priority NORMAL/IMPORTANT/URGENT
--    外键 -> ann_category.id（category_id）、sys_user.id（author_id/audit_user_id/create_by）、sys_dept.id（dept_id）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `ann_announcement`;
CREATE TABLE `ann_announcement`
(
    `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `title`         varchar(200) NOT NULL COMMENT '公告标题',
    `subtitle`      varchar(200) NULL DEFAULT NULL COMMENT '副标题',
    `summary`       varchar(500) NULL DEFAULT NULL COMMENT '摘要',
    `content`       longtext     NULL COMMENT '正文（富文本）',
    `cover_image`   varchar(255) NULL DEFAULT NULL COMMENT '封面图地址',
    `category_id`   bigint       NULL DEFAULT NULL COMMENT '分类ID 外键-> ann_category.id',
    `author_id`     bigint       NULL DEFAULT NULL COMMENT '作者ID 外键-> sys_user.id',
    `dept_id`       bigint       NULL DEFAULT NULL COMMENT '发布部门ID 外键-> sys_dept.id',
    `campus`        varchar(50)  NULL DEFAULT NULL COMMENT '所属校区',
    `status`        varchar(20)  NOT NULL DEFAULT 'DRAFT' COMMENT '状态 DRAFT/PENDING/REJECTED/SCHEDULED/PUBLISHED/WITHDRAWN/ARCHIVED',
    `visibility`    varchar(20)  NOT NULL DEFAULT 'ALL' COMMENT '可见范围 ALL/COLLEGE/CLASS/CUSTOM',
    `priority`      varchar(20)  NOT NULL DEFAULT 'NORMAL' COMMENT '优先级 NORMAL/IMPORTANT/URGENT',
    `is_top`        tinyint      NOT NULL DEFAULT 0 COMMENT '是否置顶 1是 0否',
    `is_banner`     tinyint      NOT NULL DEFAULT 0 COMMENT '是否轮播 1是 0否',
    `is_red_head`   tinyint      NOT NULL DEFAULT 0 COMMENT '是否红头文件 1是 0否',
    `allow_comment` tinyint      NOT NULL DEFAULT 1 COMMENT '是否允许评论 1允许 0禁止',
    `need_receipt`  tinyint      NOT NULL DEFAULT 0 COMMENT '是否需要回执 1需要 0不需要',
    `publish_time`  datetime     NULL DEFAULT NULL COMMENT '发布时间/计划发布时间',
    `expire_time`   datetime     NULL DEFAULT NULL COMMENT '到期时间，到期自动归档',
    `audit_user_id` bigint       NULL DEFAULT NULL COMMENT '审核人ID 外键-> sys_user.id',
    `audit_time`    datetime     NULL DEFAULT NULL COMMENT '审核时间',
    `audit_remark`  varchar(500) NULL DEFAULT NULL COMMENT '审核意见',
    `view_count`    int          NOT NULL DEFAULT 0 COMMENT '浏览次数',
    `receipt_count` int          NOT NULL DEFAULT 0 COMMENT '已读回执数',
    `target_count`  int          NOT NULL DEFAULT 0 COMMENT '应回执人数',
    `version`       int          NOT NULL DEFAULT 1 COMMENT '版本号（乐观锁）',
    `keywords`      varchar(255) NULL DEFAULT NULL COMMENT '关键词，逗号分隔',
    `create_by`     varchar(64)  NULL DEFAULT NULL COMMENT '创建人（自动填充）',
    `create_time`   datetime     NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       tinyint      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
    PRIMARY KEY (`id`),
    KEY `idx_ann_status` (`status`),
    KEY `idx_ann_category` (`category_id`),
    KEY `idx_ann_publish_time` (`publish_time`),
    KEY `idx_ann_author` (`author_id`),
    KEY `idx_ann_dept` (`dept_id`),
    KEY `idx_ann_top_banner` (`is_top`, `is_banner`),
    KEY `idx_ann_status_publish` (`status`, `publish_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='公告主表';

-- ------------------------------------------------------------
-- 6. 公告附件表 ann_attachment  外键 -> ann_announcement.id（announcement_id）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `ann_attachment`;
CREATE TABLE `ann_attachment`
(
    `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `announcement_id` bigint       NULL DEFAULT NULL COMMENT '公告ID 外键-> ann_announcement.id',
    `file_name`       varchar(255) NOT NULL COMMENT '原始文件名',
    `file_path`       varchar(500) NOT NULL COMMENT '存储路径/访问地址',
    `file_size`       bigint       NULL DEFAULT 0 COMMENT '文件大小（字节）',
    `file_type`       varchar(50)  NULL DEFAULT NULL COMMENT '文件类型/后缀',
    `download_count`  int          NOT NULL DEFAULT 0 COMMENT '下载次数',
    `create_time`     datetime     NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_attachment_announcement` (`announcement_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='公告附件表';

-- ------------------------------------------------------------
-- 7. 公告可见范围表 ann_scope  scopeType：COLLEGE/CLASS/USER
--    外键 -> ann_announcement.id（announcement_id）、sys_dept.id（scope_value，USER 时为 sys_user.id）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `ann_scope`;
CREATE TABLE `ann_scope`
(
    `id`              bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `announcement_id` bigint      NOT NULL COMMENT '公告ID 外键-> ann_announcement.id',
    `scope_type`      varchar(20) NOT NULL COMMENT '范围类型 COLLEGE/CLASS/USER',
    `scope_value`     bigint      NULL DEFAULT NULL COMMENT '范围对象ID 外键-> sys_dept.id 或 sys_user.id',
    `scope_name`      varchar(100) NULL DEFAULT NULL COMMENT '范围对象名称快照',
    `create_time`     datetime    NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_scope_announcement` (`announcement_id`),
    KEY `idx_scope_type_value` (`scope_type`, `scope_value`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='公告可见范围表';

-- ------------------------------------------------------------
-- 8. 标签表 ann_tag
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `ann_tag`;
CREATE TABLE `ann_tag`
(
    `id`          bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        varchar(50) NOT NULL COMMENT '标签名称',
    `color`       varchar(20) NULL DEFAULT NULL COMMENT '标签颜色',
    `use_count`   int         NOT NULL DEFAULT 0 COMMENT '使用次数',
    `create_time` datetime    NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tag_name` (`name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='标签表';

-- ------------------------------------------------------------
-- 9. 公告标签关联表 ann_tag_rel  外键 -> ann_announcement.id、ann_tag.id
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `ann_tag_rel`;
CREATE TABLE `ann_tag_rel`
(
    `id`              bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `announcement_id` bigint NOT NULL COMMENT '公告ID 外键-> ann_announcement.id',
    `tag_id`          bigint NOT NULL COMMENT '标签ID 外键-> ann_tag.id',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tag_rel` (`announcement_id`, `tag_id`),
    KEY `idx_tag_rel_tag` (`tag_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='公告标签关联表';

-- ------------------------------------------------------------
-- 10. 公告版本表 ann_version  外键 -> ann_announcement.id、sys_user.id（editor_id）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `ann_version`;
CREATE TABLE `ann_version`
(
    `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `announcement_id` bigint       NOT NULL COMMENT '公告ID 外键-> ann_announcement.id',
    `version_no`      int          NOT NULL DEFAULT 1 COMMENT '版本号',
    `title`           varchar(200) NULL DEFAULT NULL COMMENT '该版本标题',
    `content`         longtext     NULL COMMENT '该版本正文',
    `editor_id`       bigint       NULL DEFAULT NULL COMMENT '编辑人ID 外键-> sys_user.id',
    `editor_name`     varchar(50)  NULL DEFAULT NULL COMMENT '编辑人姓名',
    `change_log`      varchar(500) NULL DEFAULT NULL COMMENT '变更说明',
    `create_time`     datetime     NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_version_announcement` (`announcement_id`),
    KEY `idx_version_no` (`announcement_id`, `version_no`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='公告版本表';

-- ------------------------------------------------------------
-- 11. 公告评论表 ann_comment  status：PENDING/APPROVED/REJECTED
--     外键 -> ann_announcement.id、sys_user.id（user_id）、ann_comment.id（parent_id）、sys_user.id（audit_user_id）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `ann_comment`;
CREATE TABLE `ann_comment`
(
    `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `announcement_id` bigint       NOT NULL COMMENT '公告ID 外键-> ann_announcement.id',
    `user_id`         bigint       NOT NULL COMMENT '评论人ID 外键-> sys_user.id',
    `parent_id`       bigint       NULL DEFAULT 0 COMMENT '父评论ID，0为顶层 外键-> ann_comment.id',
    `content`         varchar(1000) NOT NULL COMMENT '评论内容',
    `like_count`      int          NOT NULL DEFAULT 0 COMMENT '点赞数',
    `status`          varchar(20)  NOT NULL DEFAULT 'PENDING' COMMENT '状态 PENDING/APPROVED/REJECTED',
    `audit_user_id`   bigint       NULL DEFAULT NULL COMMENT '审核人ID 外键-> sys_user.id',
    `create_time`     datetime     NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted`         tinyint      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
    PRIMARY KEY (`id`),
    KEY `idx_comment_announcement` (`announcement_id`),
    KEY `idx_comment_user` (`user_id`),
    KEY `idx_comment_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='公告评论表';

-- ------------------------------------------------------------
-- 12. 审批日志表 ann_approval_log
--     node：SUBMIT/DEPT_AUDIT/UNIV_AUDIT/PUBLISH/REJECT/WITHDRAW/ARCHIVE/UPDATE
--     外键 -> ann_announcement.id、sys_user.id（operator_id）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `ann_approval_log`;
CREATE TABLE `ann_approval_log`
(
    `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `announcement_id` bigint       NOT NULL COMMENT '公告ID 外键-> ann_announcement.id',
    `node`            varchar(20)  NOT NULL COMMENT '审批节点 SUBMIT/DEPT_AUDIT/UNIV_AUDIT/PUBLISH/REJECT/WITHDRAW/ARCHIVE/UPDATE',
    `operator_id`     bigint       NULL DEFAULT NULL COMMENT '操作人ID 外键-> sys_user.id',
    `operator_name`   varchar(50)  NULL DEFAULT NULL COMMENT '操作人姓名',
    `action`          varchar(100) NULL DEFAULT NULL COMMENT '操作描述',
    `remark`          varchar(500) NULL DEFAULT NULL COMMENT '备注/审核意见',
    `from_status`     varchar(20)  NULL DEFAULT NULL COMMENT '变更前状态',
    `to_status`       varchar(20)  NULL DEFAULT NULL COMMENT '变更后状态',
    `create_time`     datetime     NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_approval_announcement` (`announcement_id`),
    KEY `idx_approval_node` (`node`),
    KEY `idx_approval_create_time` (`create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='审批日志表';

-- ------------------------------------------------------------
-- 13. 回执表 ann_receipt  readFlag：1已读 0未读
--     外键 -> ann_announcement.id、sys_user.id（user_id）、sys_dept.id（dept_id）
--     唯一键保证同一用户对同一公告只有一条回执
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `ann_receipt`;
CREATE TABLE `ann_receipt`
(
    `id`              bigint   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `announcement_id` bigint   NOT NULL COMMENT '公告ID 外键-> ann_announcement.id',
    `user_id`         bigint   NOT NULL COMMENT '用户ID 外键-> sys_user.id',
    `dept_id`         bigint   NULL DEFAULT NULL COMMENT '用户部门ID 外键-> sys_dept.id',
    `read_flag`       tinyint  NOT NULL DEFAULT 0 COMMENT '已读标记 1已读 0未读',
    `read_time`       datetime NULL DEFAULT NULL COMMENT '阅读时间',
    `read_duration`   int      NULL DEFAULT 0 COMMENT '阅读时长（秒）',
    `urge_count`      int      NOT NULL DEFAULT 0 COMMENT '被催办次数',
    `last_urge_time`  datetime NULL DEFAULT NULL COMMENT '最近一次催办时间',
    `create_time`     datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_receipt_ann_user` (`announcement_id`, `user_id`),
    KEY `idx_receipt_read_flag` (`read_flag`),
    KEY `idx_receipt_dept` (`dept_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='公告回执表';

-- ------------------------------------------------------------
-- 14. 订阅表 ann_subscription  channel：SITE/EMAIL/SMS/WECHAT/DINGTALK
--     外键 -> sys_user.id（user_id）、ann_category.id（category_id）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `ann_subscription`;
CREATE TABLE `ann_subscription`
(
    `id`          bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     bigint      NOT NULL COMMENT '用户ID 外键-> sys_user.id',
    `category_id` bigint      NULL DEFAULT NULL COMMENT '分类ID 外键-> ann_category.id',
    `channel`     varchar(20) NOT NULL DEFAULT 'SITE' COMMENT '推送渠道 SITE/EMAIL/SMS/WECHAT/DINGTALK',
    `enabled`     tinyint     NOT NULL DEFAULT 1 COMMENT '是否启用 1启用 0停用',
    `create_time` datetime    NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_subscription` (`user_id`, `category_id`, `channel`),
    KEY `idx_subscription_user` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='公告订阅表';

-- ------------------------------------------------------------
-- 15. 推送日志表 ann_push_log  外键 -> ann_announcement.id
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `ann_push_log`;
CREATE TABLE `ann_push_log`
(
    `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `announcement_id` bigint       NOT NULL COMMENT '公告ID 外键-> ann_announcement.id',
    `channel`         varchar(20)  NOT NULL COMMENT '推送渠道 SITE/EMAIL/SMS/WECHAT/DINGTALK',
    `target_count`    int          NOT NULL DEFAULT 0 COMMENT '目标人数',
    `success_count`   int          NOT NULL DEFAULT 0 COMMENT '成功数',
    `fail_count`      int          NOT NULL DEFAULT 0 COMMENT '失败数',
    `status`          tinyint      NOT NULL DEFAULT 1 COMMENT '状态 1成功 0失败',
    `error_msg`       varchar(500) NULL DEFAULT NULL COMMENT '失败原因',
    `create_time`     datetime     NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_push_announcement` (`announcement_id`),
    KEY `idx_push_channel` (`channel`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='推送日志表';

-- ------------------------------------------------------------
-- 16. 站内通知表 sys_notice  type：SYSTEM/AUDIT/ANNOUNCE/URGE
--     外键 -> sys_user.id（user_id）、biz_id 关联公告ID
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_notice`;
CREATE TABLE `sys_notice`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     bigint       NOT NULL COMMENT '接收人ID 外键-> sys_user.id',
    `title`       varchar(200) NOT NULL COMMENT '通知标题',
    `content`     varchar(500) NULL DEFAULT NULL COMMENT '通知内容',
    `type`        varchar(20)  NOT NULL DEFAULT 'SYSTEM' COMMENT '类型 SYSTEM/AUDIT/ANNOUNCE/URGE',
    `biz_id`      bigint       NULL DEFAULT NULL COMMENT '业务ID，关联 ann_announcement.id',
    `read_flag`   tinyint      NOT NULL DEFAULT 0 COMMENT '已读标记 1已读 0未读',
    `create_time` datetime     NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_notice_user_read` (`user_id`, `read_flag`),
    KEY `idx_notice_type` (`type`),
    KEY `idx_notice_create_time` (`create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='站内通知表';

-- ------------------------------------------------------------
-- 17. 操作日志表 sys_operation_log  外键 -> sys_user.id（user_id）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_operation_log`;
CREATE TABLE `sys_operation_log`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     bigint       NULL DEFAULT NULL COMMENT '操作人ID 外键-> sys_user.id',
    `username`    varchar(50)  NULL DEFAULT NULL COMMENT '操作人登录名',
    `module`      varchar(50)  NULL DEFAULT NULL COMMENT '所属模块',
    `operation`   varchar(100) NULL DEFAULT NULL COMMENT '操作描述',
    `method`      varchar(200) NULL DEFAULT NULL COMMENT '请求方法（类名.方法名）',
    `params`      text         NULL COMMENT '请求参数',
    `ip`          varchar(64)  NULL DEFAULT NULL COMMENT '客户端IP',
    `location`    varchar(100) NULL DEFAULT NULL COMMENT 'IP归属地',
    `cost_time`   bigint       NULL DEFAULT 0 COMMENT '耗时（毫秒）',
    `status`      tinyint      NOT NULL DEFAULT 1 COMMENT '状态 1成功 0失败',
    `error_msg`   varchar(1000) NULL DEFAULT NULL COMMENT '异常信息',
    `create_time` datetime     NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_log_user` (`user_id`),
    KEY `idx_log_module` (`module`),
    KEY `idx_log_create_time` (`create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='操作日志表';

-- ------------------------------------------------------------
-- 18. 敏感词表 sys_sensitive_word  level：BLOCK 阻断 / WARN 警告
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_sensitive_word`;
CREATE TABLE `sys_sensitive_word`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `word`        varchar(100) NOT NULL COMMENT '敏感词',
    `level`       varchar(20)  NOT NULL DEFAULT 'WARN' COMMENT '等级 BLOCK 阻断 / WARN 警告',
    `replacement` varchar(100) NULL DEFAULT NULL COMMENT '替换文本',
    `status`      tinyint      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    `create_time` datetime     NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sensitive_word` (`word`),
    KEY `idx_sensitive_level` (`level`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='敏感词表';

-- ------------------------------------------------------------
-- 19. 公告模板表 ann_template  外键 -> ann_category.id（category_id）、sys_user.id（creator_id）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `ann_template`;
CREATE TABLE `ann_template`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        varchar(100) NOT NULL COMMENT '模板名称',
    `category_id` bigint       NULL DEFAULT NULL COMMENT '分类ID 外键-> ann_category.id',
    `title`       varchar(200) NULL DEFAULT NULL COMMENT '预置标题',
    `content`     longtext     NULL COMMENT '预置正文',
    `use_count`   int          NOT NULL DEFAULT 0 COMMENT '使用次数',
    `creator_id`  bigint       NULL DEFAULT NULL COMMENT '创建人ID 外键-> sys_user.id',
    `status`      tinyint      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    `create_time` datetime     NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_template_category` (`category_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='公告模板表';

SET FOREIGN_KEY_CHECKS = 1;
