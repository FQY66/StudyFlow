package com.sf.mapper;

import com.sf.entity.PrivateMessage;
import com.sf.vo.PrivateMessageVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface PrivateMessageMapper {
    void insert(PrivateMessage message);

    void markConversationAsRead(Long userId, Long friendId);

    List<PrivateMessageVO> listConversation(Long userId, Long friendId);
}
