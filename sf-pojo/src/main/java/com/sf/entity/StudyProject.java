package com.sf.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudyProject implements Serializable {
    /** 主键ID */
    private Long id;

    /** 项目标题 */
    private String title;
    /** 项目分类 */
    private String category;
    /** 项目简介（列表摘要） */
    private String summary;
    /** 项目详情内容 */
    private String content;
    /** 封面URL */
    private String cover;

    /** 开始日期 */
    private LocalDate startDate;
    /** 结束日期 */
    private LocalDate endDate;

    /** 名额上限 */
    private Integer capacity;

    /**
     * 项目状态：DRAFT（草稿）/ PUBLISHED（已发布）/ CLOSED（已结束）
     */
    private String status;

    /** 创建人ID */
    private Long creatorId;

    /** 创建时间 */
    private LocalDateTime createTime;
    /** 更新时间 */
    private LocalDateTime updateTime;
}

