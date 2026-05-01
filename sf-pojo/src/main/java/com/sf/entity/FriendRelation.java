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
public class FriendRelation implements Serializable {
    private Long id;
    private Long userId;
    private Long friendId;
    private Integer status; // 0-待处理 1-已通过 2-已拒绝
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
