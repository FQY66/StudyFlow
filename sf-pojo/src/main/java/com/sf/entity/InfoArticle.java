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
public class InfoArticle implements Serializable {
    /** 主键ID */
    private Long id;

    /** 文章标题 */
    private String title;
    /** 文章分类 */
    private String category;
    /** 文章内容 */
    private String content;
    /** 封面URL */
    private String cover;

    /** 发布人ID */
    private Long publisherId;

    /**
     * 审核状态：PENDING（待审核）/ APPROVED（已通过）/ REJECTED（已拒绝）
     */
    private String auditStatus;

    /** 创建时间 */
    private LocalDateTime createTime;
    /** 更新时间 */
    private LocalDateTime updateTime;
}

