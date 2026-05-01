package com.sf.service.impl;

import com.sf.context.BaseContext;
import com.sf.dto.FriendRequestDTO;
import com.sf.dto.PrivateMessageDTO;
import com.sf.entity.FriendRelation;
import com.sf.entity.PrivateMessage;
import com.sf.mapper.FriendMapper;
import com.sf.mapper.PrivateMessageMapper;
import com.sf.mapper.UserMapper;
import com.sf.service.FriendChatService;
import com.sf.service.OnlineStatusService;
import com.sf.vo.FriendRequestVO;
import com.sf.vo.FriendVO;
import com.sf.vo.PrivateMessageVO;
import com.sf.websocket.ChatWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class FriendChatServiceImpl implements FriendChatService {

    @Autowired
    private FriendMapper friendMapper;

    @Autowired
    private PrivateMessageMapper privateMessageMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OnlineStatusService onlineStatusService;

    @Override
    public void addFriend(FriendRequestDTO dto) {
        Long userId = BaseContext.getCurrentId();
        Long friendId = dto.getFriendId();
        if (userId == null || friendId == null || userId.equals(friendId)) {
            return;
        }
        if (userMapper.getByUserId(friendId) == null) {
            return;
        }

        FriendRelation relation = friendMapper.findRelation(userId, friendId);
        if (relation == null) {
            friendMapper.insertRelation(FriendRelation.builder()
                    .userId(userId)
                    .friendId(friendId)
                    .status(0)
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build());
            return;
        }

        if (relation.getStatus() != null && relation.getStatus() == 1) {
            return;
        }
        if (relation.getStatus() != null && relation.getStatus() == 0) {
            return;
        }

        friendMapper.updateRelationStatus(relation.getId(), 0);
    }

    @Override
    public void approveFriend(Long requestId) {
        Long userId = BaseContext.getCurrentId();
        if (requestId == null || userId == null) {
            return;
        }
        FriendRelation relation = friendMapper.findRelationByIdAndUserId(requestId, userId);
        if (relation == null || relation.getStatus() == null || relation.getStatus() != 0) {
            return;
        }
        friendMapper.updateRelationStatus(requestId, 1);
        FriendRelation updated = friendMapper.findRelationById(requestId);
        if (updated != null) {
            ChatWebSocketHandler.sendToUser(updated.getUserId(), "{\"type\":\"friend_request_approved\",\"requestId\":" + requestId + "}");
        }
    }

    @Override
    public void rejectFriend(Long requestId) {
        Long userId = BaseContext.getCurrentId();
        if (requestId == null || userId == null) {
            return;
        }
        FriendRelation relation = friendMapper.findRelationByIdAndUserId(requestId, userId);
        if (relation == null || relation.getStatus() == null || relation.getStatus() != 0) {
            return;
        }
        friendMapper.updateRelationStatus(requestId, 2);
        FriendRelation updated = friendMapper.findRelationById(requestId);
        if (updated != null) {
            ChatWebSocketHandler.sendToUser(updated.getUserId(), "{\"type\":\"friend_request_rejected\",\"requestId\":" + requestId + "}");
        }
    }

    @Override
    public List<FriendVO> listFriends() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return Collections.emptyList();
        }
        List<FriendVO> friends = friendMapper.listFriends(userId);
        if (friends == null || friends.isEmpty()) {
            return Collections.emptyList();
        }
        friends.forEach(friend -> friend.setOnline(onlineStatusService.isOnline(friend.getUserId())));
        return friends;
    }

    @Override
    public List<FriendRequestVO> listPendingRequests() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return Collections.emptyList();
        }
        return friendMapper.listPendingRequests(userId);
    }

    @Override
    public void sendMessage(PrivateMessageDTO dto) {
        Long senderId = BaseContext.getCurrentId();
        sendMessageByUserId(senderId, dto);
    }

    @Override
    public void sendMessageByUserId(Long senderId, PrivateMessageDTO dto) {
        if (senderId == null || dto == null || dto.getToUserId() == null || dto.getContent() == null || dto.getContent().trim().isEmpty()) {
            return;
        }

        FriendRelation relation = friendMapper.findRelation(senderId, dto.getToUserId());
        if (relation == null || relation.getStatus() == null || relation.getStatus() != 1) {
            return;
        }

        PrivateMessage message = PrivateMessage.builder()
                .senderId(senderId)
                .receiverId(dto.getToUserId())
                .content(dto.getContent().trim())
                .isRead(0)
                .createTime(LocalDateTime.now())
                .build();
        privateMessageMapper.insert(message);
    }

    @Override
    public List<PrivateMessageVO> listConversation(Long friendId) {
        Long userId = BaseContext.getCurrentId();
        return listConversationByUserId(userId, friendId);
    }

    @Override
    public List<PrivateMessageVO> listConversationByUserId(Long userId, Long friendId) {
        if (userId == null || friendId == null) {
            return Collections.emptyList();
        }
        FriendRelation relation = friendMapper.findRelation(userId, friendId);
        if (relation == null || relation.getStatus() == null || relation.getStatus() != 1) {
            return Collections.emptyList();
        }
        return privateMessageMapper.listConversation(userId, friendId);
    }
}
