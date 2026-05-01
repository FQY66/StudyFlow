package com.sf.mapper;

import com.sf.entity.FriendRelation;
import com.sf.vo.FriendRequestVO;
import com.sf.vo.FriendVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface FriendMapper {
    FriendRelation findRelation(Long userId, Long friendId);

    FriendRelation findRelationById(Long id);

    FriendRelation findRelationByIdAndUserId(Long id, Long userId);

    void insertRelation(FriendRelation relation);

    void updateRelationStatus(Long id, Integer status);

    List<FriendVO> listFriends(Long userId);

    List<FriendRequestVO> listPendingRequests(Long userId);
}
