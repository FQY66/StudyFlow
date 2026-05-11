package com.sf.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sf.dto.PrivateMessageDTO;
import com.sf.service.FriendChatService;
import com.sf.service.OnlineStatusService;
import com.sf.vo.PrivateMessageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Map<Long, WebSocketSession> sessionMap = new ConcurrentHashMap<>();

    public static boolean isOnline(Long userId) {
        if (userId == null) {
            return false;
        }
        WebSocketSession session = sessionMap.get(userId);
        return session != null && session.isOpen();
    }

    public static int countOnlineUsers() {
        return sessionMap.size();
    }

    public static boolean hasActiveSession(Long userId) {
        if (userId == null) {
            return false;
        }
        WebSocketSession session = sessionMap.get(userId);
        return session != null && session.isOpen();
    }

    public static void sendToUser(Long userId, String payload) {
        if (userId == null || payload == null) {
            return;
        }
        WebSocketSession session = sessionMap.get(userId);
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            session.sendMessage(new TextMessage(payload));
        } catch (Exception e) {
            log.warn("WebSocket push failed: userId={}, reason={}", userId, e.getMessage());
        }
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private FriendChatService friendChatService;

    @Autowired
    private OnlineStatusService onlineStatusService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            sessionMap.put(userId, session);
            onlineStatusService.markOnline(userId);
            log.info("WebSocket connected: userId={}, onlineCount={}", userId, sessionMap.size());
        } else {
            log.warn("WebSocket connected without userId, sessionId={}", session.getId());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId == null) {
            log.warn("WebSocket message ignored: missing userId, sessionId={}", session.getId());
            return;
        }

        WsChatReq req = objectMapper.readValue(message.getPayload(), WsChatReq.class);
        if (req == null || req.getToUserId() == null || req.getContent() == null || req.getContent().trim().isEmpty()) {
            log.warn("WebSocket message invalid: fromUserId={}, payload={}", userId, message.getPayload());
            return;
        }

        WebSocketSession toSession = sessionMap.get(req.getToUserId());
        boolean bothOnline = toSession != null && toSession.isOpen() && session.isOpen();
        if (bothOnline) {
            log.info("WebSocket peer-ready: fromUserId={}, toUserId={}, bothOnline=true", userId, req.getToUserId());
        } else {
            log.info("WebSocket peer-not-ready: fromUserId={}, toUserId={}, bothOnline=false", userId, req.getToUserId());
        }

        friendChatService.sendMessageByUserId(userId, PrivateMessageDTO.builder()
                .toUserId(req.getToUserId())
                .content(req.getContent())
                .build());

        List<PrivateMessageVO> list = friendChatService.listConversationByUserId(userId, req.getToUserId());
        if (list.isEmpty()) {
            log.warn("WebSocket message persisted but conversation empty: fromUserId={}, toUserId={}", userId, req.getToUserId());
            return;
        }
        PrivateMessageVO latest = list.get(list.size() - 1);

        String payload = objectMapper.writeValueAsString(latest);
        session.sendMessage(new TextMessage(payload));

        if (toSession != null && toSession.isOpen()) {
            toSession.sendMessage(new TextMessage(payload));
            log.info("WebSocket delivered: msgId={}, fromUserId={}, toUserId={}", latest.getId(), userId, req.getToUserId());
        } else {
            log.info("WebSocket receiver offline: msgId={}, fromUserId={}, toUserId={}", latest.getId(), userId, req.getToUserId());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            sessionMap.remove(userId);
            onlineStatusService.markOffline(userId);
            log.info("WebSocket disconnected: userId={}, onlineCount={}, status={}", userId, sessionMap.size(), status);
        }
    }
}
