-- StudyFlow 数据库建表脚本（MySQL 5.7）
-- 仅包含：研学资讯、精品课程

CREATE TABLE IF NOT EXISTS `research_news` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` VARCHAR(200) NOT NULL COMMENT '资讯标题',
  `category` VARCHAR(50) NOT NULL COMMENT '资讯分类',
  `summary` VARCHAR(500) DEFAULT NULL COMMENT '资讯摘要',
  `content` LONGTEXT NOT NULL COMMENT '资讯内容',
  `cover_path` VARCHAR(255) DEFAULT NULL COMMENT '封面地址',
  `source` VARCHAR(100) DEFAULT NULL COMMENT '来源',
  `status` VARCHAR(20) NOT NULL DEFAULT '已发布' COMMENT '状态：草稿/已发布/已下架',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序权重',
  `click_count` INT NOT NULL DEFAULT 0 COMMENT '点击量',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_research_news_category` (`category`),
  KEY `idx_research_news_status` (`status`),
  KEY `idx_research_news_sort` (`sort_order`),
  KEY `idx_research_news_time` (`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='研学资讯表';

CREATE TABLE IF NOT EXISTS `premium_course` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` VARCHAR(200) NOT NULL COMMENT '课程标题',
  `category` VARCHAR(50) NOT NULL COMMENT '课程分类',
  `summary` VARCHAR(500) DEFAULT NULL COMMENT '课程摘要',
  `content` LONGTEXT NOT NULL COMMENT '课程内容',
  `cover_path` VARCHAR(255) DEFAULT NULL COMMENT '封面地址',
  `video_path` VARCHAR(255) DEFAULT NULL COMMENT '视频资源路径',
  `teacher_name` VARCHAR(100) DEFAULT NULL COMMENT '讲师名称',
  `duration` VARCHAR(50) DEFAULT NULL COMMENT '课程时长',
  `status` VARCHAR(20) NOT NULL DEFAULT '已发布' COMMENT '状态：草稿/已发布/已下架',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序权重',
  `click_count` INT NOT NULL DEFAULT 0 COMMENT '点击量',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_premium_course_category` (`category`),
  KEY `idx_premium_course_status` (`status`),
  KEY `idx_premium_course_sort` (`sort_order`),
  KEY `idx_premium_course_time` (`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='精品课程表';

-- =========================
-- 初始化数据
-- =========================
INSERT INTO `research_news` (`title`, `category`, `summary`, `content`, `cover_path`, `source`, `status`, `sort_order`, `click_count`, `create_time`, `update_time`) VALUES
('研学旅行课程建设的实践路径', '研学资讯', '围绕研学课程设计、实施和评价的实操建议。', '本文从研学课程目标、活动任务设计、过程评价和成果展示四个方面，分享一套可直接落地的课程建设思路。', 'https://picsum.photos/seed/news1/800/450', 'StudyFlow 编辑部', '已发布', 10, 128, NOW(), NOW()),
('中小学研学安全管理要点', '安全管理', '聚焦研学活动中的安全保障、应急预案与风险识别。', '研学活动开展前应完成风险评估、路线勘查、人员分工和应急物资准备，确保活动全流程安全可控。', 'https://picsum.photos/seed/news2/800/450', '教育观察', '已发布', 8, 96, NOW(), NOW()),
('研学成果展示如何更有温度', '成果展示', '让学生在展示中表达学习收获，提升研学成效。', '通过作品展、路演、海报和微视频等形式，可以让学生将研学中的观察、思考与表达结合起来。', 'https://picsum.photos/seed/news3/800/450', '研学研究中心', '已发布', 6, 74, NOW(), NOW()),
('研学基地课程资源共建经验分享', '基地建设', '来自基地一线的课程资源共建经验。', '课程资源共建的关键在于建立学校、基地和行业导师协同机制，形成可持续迭代的资源体系。', 'https://picsum.photos/seed/news4/800/450', '研学联盟', '草稿', 4, 21, NOW(), NOW()),
('跨学科研学项目的设计方法', '课程设计', '将语文、历史、地理与劳动教育融入研学任务。', '跨学科研学强调真实问题驱动，通过主题任务、分组协作和成果表达，让学生在实践中完成综合学习。', 'https://picsum.photos/seed/news5/800/450', '课程教研组', '已下架', 2, 15, NOW(), NOW());

INSERT INTO `premium_course` (`title`, `category`, `summary`, `content`, `cover_path`, `video_path`, `teacher_name`, `duration`, `status`, `sort_order`, `click_count`, `create_time`, `update_time`) VALUES
('研学课程设计入门', '课程设计', '带你快速掌握研学课程设计的基本框架。', '课程内容包括课程目标设定、任务单编写、活动流程设计以及成果评价标准，适合研学课程初学者。', '/static/courses/course1-cover.jpg', '/static/courses/course1.mp4', '张老师', '2小时30分钟', '已发布', 10, 156, NOW(), NOW()),
('研学安全与应急管理', '安全管理', '系统讲解研学活动安全管理的重点。', '从出行准备、现场管理、突发事件处理到复盘改进，完整梳理研学安全管理的关键动作。', '/static/courses/course2-cover.jpg', '/static/courses/course2.mp4', '李老师', '1小时45分钟', '已发布', 9, 122, NOW(), NOW()),
('研学成果评价实战', '成果评价', '帮助教师建立更科学的研学评价体系。', '围绕过程性评价、表现性评价和成果展示评价，提供评价量表和案例分析。', '/static/courses/course3-cover.jpg', '/static/courses/course3.mp4', '王老师', '3小时', '已发布', 8, 98, NOW(), NOW()),
('研学基地运营与课程开发', '基地运营', '适合基地运营人员和课程负责人学习。', '课程聚焦基地资源盘点、路线设计、课程包装和运营协同，提升基地课程的持续供给能力。', '/static/courses/course4-cover.jpg', '/static/courses/course4.mp4', '陈老师', '2小时', '待审核', 6, 42, NOW(), NOW()),
('跨学科研学项目案例集', '案例分享', '通过真实案例学习项目化研学设计。', '精选多个跨学科研学案例，拆解其主题来源、任务设计、过程实施和成果呈现方式。', '/static/courses/course5-cover.jpg', '/static/courses/course5.mp4', '赵老师', '2小时15分钟', '已下架', 4, 33, NOW(), NOW());
