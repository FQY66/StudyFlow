package com.sf.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForumPostDTO {

    /** 帖子ID */
    private Long id;

    /** 标题 */
    private String title;

    /** 内容摘要（前端列表展示使用） */
    private String content;

    /** 类别/标签 */
    private String category;

    /** 作者名称 */
    private String authorName;

    /** 创建日期字符串，例如 2024-09-03 */
    private String createdAt;

    /** 评论数 */
    private Integer commentCount;

    /** 点赞数 */
    private Integer likeCount;
}

