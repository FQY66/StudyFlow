package entity;

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
public class ForumPost implements Serializable {
    /** 主键ID */
    private Long id;

    /** 帖子标题 */
    private String title;
    /** 帖子内容 */
    private String content;
    /** 标签（可用逗号分隔） */
    private String tags;

    /** 发帖人ID */
    private Long authorId;

    /** 点赞数 */
    private Integer likeCount;

    /**
     * 审核状态：PENDING（待审核）/ APPROVED（已通过）/ REJECTED（已拒绝）
     */
    private String auditStatus;

    /** 创建时间 */
    private LocalDateTime createTime;
    /** 更新时间 */
    private LocalDateTime updateTime;
}

