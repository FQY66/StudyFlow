package com.sf.vo;

import com.sf.dto.ForumCommentDTO;
import com.sf.dto.ForumPostDTO;
import com.sf.entity.ForumComment;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ForumPostVO {

    /** 主键ID */
    private Long id;
    /** 帖子标题 */
    private String title;
    /** 帖子内容 */
    private String content;
    /** 类别 */
    private String category;

    /** 评论数 */
    private Integer commentCount;

    /** 发帖人ID */
    private Long authorId;

    /** 点赞数 */
    private Integer likeCount;

    /** 更新时间 */
    private LocalDateTime updateTime;
    /** 评论列表 */
    private List<ForumComment> comments = new ArrayList<>();
}
