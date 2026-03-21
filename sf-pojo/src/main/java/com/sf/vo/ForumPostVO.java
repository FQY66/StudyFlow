package com.sf.vo;

import com.sf.entity.ForumComment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForumPostVO implements Serializable {

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
    /** 发帖人姓名 */
    private String authorName;
    /** 发帖人头像 */
    private String authorAvatar;
    /** 点赞数 */
    private Integer likeCount;
    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 评论列表 */
    private List<ForumCommentVO> comments = new ArrayList<>();
}
