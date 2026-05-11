package com.sf.controller.common;

import com.sf.dto.FriendRequestDTO;
import com.sf.dto.PrivateMessageDTO;
import com.sf.result.Result;
import com.sf.service.FriendChatService;
import com.sf.service.OnlineStatusService;
import com.sf.vo.FriendVO;
import com.sf.vo.PrivateMessageVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chat")
public class FriendChatController {

    @Autowired
    private FriendChatService friendChatService;

    @Autowired
    private OnlineStatusService onlineStatusService;

    @PutMapping("/friend/add")
    public Result addFriend(@RequestBody FriendRequestDTO dto) {
        friendChatService.addFriend(dto);
        return Result.success();
    }

    @PutMapping("/friend/approve")
    public Result approveFriend(@RequestParam Long requestId) {
        friendChatService.approveFriend(requestId);
        return Result.success();
    }

    @PutMapping("/friend/reject")
    public Result rejectFriend(@RequestParam Long requestId) {
        friendChatService.rejectFriend(requestId);
        return Result.success();
    }

    @GetMapping("/friend/requests")
    public Result<List<com.sf.vo.FriendRequestVO>> listPendingRequests() {
        return Result.success(friendChatService.listPendingRequests());
    }

    @GetMapping("/friends")
    public Result<List<FriendVO>> listFriends() {
        return Result.success(friendChatService.listFriends());
    }

    @PutMapping("/message/send")
    public Result sendMessage(@RequestBody PrivateMessageDTO dto) {
        friendChatService.sendMessage(dto);
        return Result.success();
    }

    @PostMapping("/share/project")
    public Result shareProject(@RequestBody java.util.Map<String, Object> body) {
        Long toUserId = body.get("toUserId") == null ? null : Long.valueOf(String.valueOf(body.get("toUserId")));
        Long senderId = com.sf.context.BaseContext.getCurrentId();
        String title = String.valueOf(body.getOrDefault("title", "未命名项目"));
        String summary = String.valueOf(body.getOrDefault("summary", "查看项目详情"));
        String cover = String.valueOf(body.getOrDefault("cover", ""));
        String link = String.valueOf(body.getOrDefault("link", ""));
        Long projectId = body.get("projectId") == null ? null : Long.valueOf(String.valueOf(body.get("projectId")));
        if (senderId == null || toUserId == null || projectId == null) {
            return Result.error("参数错误");
        }
        String payload = "{" +
                "\"type\":\"project_share\"," +
                "\"projectId\":" + projectId + "," +
                "\"title\":\"" + title.replace("\"", "\\\"") + "\"," +
                "\"summary\":\"" + summary.replace("\"", "\\\"") + "\"," +
                "\"cover\":\"" + cover.replace("\"", "\\\"") + "\"," +
                "\"link\":\"" + link.replace("\"", "\\\"") + "\"}";
        friendChatService.sendShareMessageByUserId(senderId, toUserId, payload);
        return Result.success();
    }

    @GetMapping("/conversation")
    public Result<List<PrivateMessageVO>> listConversation(@RequestParam Long friendId) {
        return Result.success(friendChatService.listConversation(friendId));
    }

    @GetMapping("/friend/online")
    public Result<Boolean> isFriendOnline(@RequestParam Long friendId) {
        return Result.success(onlineStatusService.isOnline(friendId));
    }
}
