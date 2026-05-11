package com.sf.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendVO {
    private Long userId;
    private String name;
    private String username;
    private String email;
    private String avatar;
    private Boolean online;
    private String lastMessage;
    private String lastMessageTime;
    private Integer unreadCount;
    private Integer lastMessageIsRead;
}
