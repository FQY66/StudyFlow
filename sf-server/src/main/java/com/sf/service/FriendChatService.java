package com.sf.service;

import com.sf.dto.FriendRequestDTO;
import com.sf.dto.PrivateMessageDTO;
import com.sf.vo.FriendVO;
import com.sf.vo.PrivateMessageVO;

import java.util.List;

public interface FriendChatService {
    void addFriend(FriendRequestDTO dto);

    void approveFriend(Long requestId);

    void rejectFriend(Long requestId);

    List<FriendVO> listFriends();

    List<com.sf.vo.FriendRequestVO> listPendingRequests();

    void sendMessage(PrivateMessageDTO dto);

    void sendMessageByUserId(Long senderId, PrivateMessageDTO dto);

    List<PrivateMessageVO> listConversation(Long friendId);

    List<PrivateMessageVO> listConversationByUserId(Long userId, Long friendId);
}
