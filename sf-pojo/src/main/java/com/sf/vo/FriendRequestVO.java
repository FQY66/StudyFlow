package com.sf.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendRequestVO {
    private Long id;
    private Long requesterId;
    private String requesterName;
    private String requesterUsername;
    private String requesterEmail;
    private String requesterAvatar;
    private Integer status;
    private LocalDateTime createTime;
}
