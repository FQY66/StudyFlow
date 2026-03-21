package com.sf.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForumCommentDTO {


    /** 所属帖子ID */
    private Long postId;

    /** 评论人名称 */
    private String authorName;

    /** 评论内容 */
    private String content;

}

