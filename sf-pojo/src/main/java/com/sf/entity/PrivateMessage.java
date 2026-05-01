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
public class PrivateMessage implements Serializable {
    private Long id;
    private Long senderId;
    private Long receiverId;
    private String content;
    private Integer isRead;
    private LocalDateTime createTime;
}
