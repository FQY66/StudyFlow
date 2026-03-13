-- StudyFlow 数据库建表脚本（MySQL 8+）
-- 默认库名：study_flow（可按需修改）

CREATE DATABASE IF NOT EXISTS `study_flow`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE `study_flow`;

-- =========================
-- 用户表
-- 对应实体：entity.User
-- =========================
-- CREATE TABLE IF NOT EXISTS `user` (
--   `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
--   `username` VARCHAR(50) NOT NULL COMMENT '用户名',
--   `password` VARCHAR(255) NOT NULL COMMENT '密码（建议存储加密后的密文）',
--   `sex` VARCHAR(10) DEFAULT NULL COMMENT '性别',
--   `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
--   `phone` VARCHAR(30) DEFAULT NULL COMMENT '手机号',
--   `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
--   `role` VARCHAR(20) NOT NULL DEFAULT 'STUDENT' COMMENT '角色（STUDENT/TEACHER/ADMIN）',
--   `status` INT NOT NULL DEFAULT 1 COMMENT '账号状态（1启用，0禁用）',
--   `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
--   `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
--   PRIMARY KEY (`id`),
--   UNIQUE KEY `uk_user_username` (`username`),
--   KEY `idx_user_role` (`role`),
--   KEY `idx_user_status` (`status`)
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- =========================
-- 研学项目表
-- 对应实体：entity.StudyProject
-- =========================
CREATE TABLE IF NOT EXISTS `study_project` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` VARCHAR(120) NOT NULL COMMENT '项目标题',
  `category` VARCHAR(50) NOT NULL COMMENT '项目分类',
  `summary` VARCHAR(255) DEFAULT NULL COMMENT '项目简介（列表摘要）',
  `content` TEXT DEFAULT NULL COMMENT '项目详情内容',
  `cover` VARCHAR(255) DEFAULT NULL COMMENT '封面URL',
  `start_date` DATE DEFAULT NULL COMMENT '开始日期',
  `end_date` DATE DEFAULT NULL COMMENT '结束日期',
  `capacity` INT NOT NULL DEFAULT 0 COMMENT '名额上限',
  `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '项目状态：DRAFT（草稿）/ PUBLISHED（已发布）/ CLOSED（已结束）',
  `creator_id` BIGINT NOT NULL COMMENT '创建人ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_project_category` (`category`),
  KEY `idx_project_status` (`status`),
  KEY `idx_project_creator` (`creator_id`),
  KEY `idx_project_time` (`start_date`, `end_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='研学项目表';

-- =========================
-- 报名表
-- 对应实体：entity.ProjectSignup
-- =========================
CREATE TABLE IF NOT EXISTS `project_signup` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `project_id` BIGINT NOT NULL COMMENT '研学项目ID',
  `user_id` BIGINT NOT NULL COMMENT '报名用户ID',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '报名状态：PENDING（待审核）/ APPROVED（已通过）/ REJECTED（已拒绝）/ CANCELLED（已取消）',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注（可用于审核说明/取消原因等）',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（报名时间）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_signup_project_user` (`project_id`, `user_id`),
  KEY `idx_signup_user` (`user_id`),
  KEY `idx_signup_status` (`status`),
  KEY `idx_signup_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='研学项目报名表';

-- =========================
-- 论坛帖子表
-- 对应实体：entity.ForumPost
-- =========================
CREATE TABLE IF NOT EXISTS `forum_post` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` VARCHAR(120) NOT NULL COMMENT '帖子标题',
  `content` TEXT NOT NULL COMMENT '帖子内容',
  `tags` VARCHAR(255) DEFAULT NULL COMMENT '标签（可用逗号分隔）',
  `author_id` BIGINT NOT NULL COMMENT '发帖人ID',
  `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数',
  `audit_status` VARCHAR(20) NOT NULL DEFAULT 'APPROVED' COMMENT '审核状态：PENDING（待审核）/ APPROVED（已通过）/ REJECTED（已拒绝）',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_post_author` (`author_id`),
  KEY `idx_post_audit` (`audit_status`),
  KEY `idx_post_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='论坛帖子表';

-- =========================
-- 论坛评论表
-- 对应实体：entity.ForumComment
-- =========================
CREATE TABLE IF NOT EXISTS `forum_comment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `post_id` BIGINT NOT NULL COMMENT '所属帖子ID',
  `content` VARCHAR(1000) NOT NULL COMMENT '评论内容',
  `author_id` BIGINT NOT NULL COMMENT '评论人ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_comment_post` (`post_id`),
  KEY `idx_comment_author` (`author_id`),
  KEY `idx_comment_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='论坛评论表';

-- =========================
-- 信息发布（文章）表
-- 对应实体：entity.InfoArticle
-- =========================
CREATE TABLE IF NOT EXISTS `info_article` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` VARCHAR(120) NOT NULL COMMENT '文章标题',
  `category` VARCHAR(50) NOT NULL COMMENT '文章分类',
  `content` TEXT NOT NULL COMMENT '文章内容',
  `cover` VARCHAR(255) DEFAULT NULL COMMENT '封面URL',
  `publisher_id` BIGINT NOT NULL COMMENT '发布人ID',
  `audit_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '审核状态：PENDING（待审核）/ APPROVED（已通过）/ REJECTED（已拒绝）',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_article_category` (`category`),
  KEY `idx_article_audit` (`audit_status`),
  KEY `idx_article_publisher` (`publisher_id`),
  KEY `idx_article_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='信息发布文章表';


