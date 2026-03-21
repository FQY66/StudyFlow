package com.sf.vo;

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
public class ForumCommentVO implements Serializable {

    //评论内容
    private String content;
    //评论时间
    private LocalDateTime createTime;
    //评论人ID
    private Long authorId;
    //评论人姓名
    private String authorName;
    //评论人头像
    private String authorAvatar;
    //评论帖子ID
    private Long postId;
    //父评论ID
    private Long parentId;
}
