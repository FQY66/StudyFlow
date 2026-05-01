package com.sf.websocket;

import lombok.Data;

@Data
public class WsChatReq {
    private Long toUserId;
    private String content;
}
