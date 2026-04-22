package com.sf.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectSignupUserVO implements Serializable {
    private Integer userId;
    private String avatar;
    private String name;
    private String status;
    private String role;
    private String remark;
    private LocalDateTime createTime;
}
