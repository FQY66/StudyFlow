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
public class ProjectSignup implements Serializable {
    /** 主键ID */
    private Long id;

    /** 研学项目ID */
    private Long projectId;
    /** 报名用户ID */
    private Long userId;

    /**
     * 报名状态：PENDING（待审核）/ APPROVED（已通过）/ REJECTED（已拒绝）
     */
    private String status;

    /** 备注（可用于审核说明/取消原因等） */
    private String remark;

    /** 创建时间（报名时间） */
    private LocalDateTime createTime;
}

