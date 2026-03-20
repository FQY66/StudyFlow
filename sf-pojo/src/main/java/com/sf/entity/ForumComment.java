package com.sf.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForumComment implements Serializable {
    /** 主键ID */
    private Long id;

    /** 所属帖子ID */
    private Long postId;
    /** 评论内容 */
    private String content;
    /** 评论人ID */
    private Long authorId;

    /** 创建时间 */
    private LocalDateTime createTime;
}

