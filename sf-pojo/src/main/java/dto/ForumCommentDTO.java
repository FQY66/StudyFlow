package dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForumCommentDTO {

    /** 评论ID */
    private Long id;

    /** 所属帖子ID */
    private Long postId;

    /** 评论人名称 */
    private String authorName;

    /** 评论内容 */
    private String content;

    /** 创建时间字符串，例如 2024-09-03 11:20 */
    private String createdAt;
}

