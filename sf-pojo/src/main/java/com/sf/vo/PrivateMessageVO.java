package com.sf.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;



@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrivateMessageVO {
    private Long id;
    private Long senderId;
    private Long receiverId;
    private String content;
    private Integer isRead;
    private String createTime;
}
