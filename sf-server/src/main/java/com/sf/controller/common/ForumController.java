package com.sf.controller.common;

import com.sf.dto.ForumCommentDTO;
import com.sf.dto.ForumPageQueryDTO;
import com.sf.dto.ForumPostDTO;
import com.sf.result.PageResult;
import com.sf.result.Result;
import com.sf.service.ForumPostService;
import com.sf.vo.ForumPostVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/forum")
@Slf4j
public class ForumController {

    @Autowired
    private ForumPostService forumPostService;

    /** 分页查询帖子（返回帖子详情结构，包含评论） */
    @GetMapping("/page")
    public Result<PageResult> page(ForumPageQueryDTO forumPageQueryDTO) {
        log.info("分页查询帖子controller层{}, page={}, pageSize={}", forumPageQueryDTO, forumPageQueryDTO.getPage(), forumPageQueryDTO.getPageSize());
        PageResult pageResult = forumPostService.pageQuery(forumPageQueryDTO);
        log.info("分页查询帖子controller层成功，给前端的结果：{}", pageResult);
        return Result.success(pageResult);
    }

    /** 帖子详情（包含评论） */
    @GetMapping("/detail")
    public Result<ForumPostVO> getPostDetail(@RequestParam Long postId) {
        log.info("查询帖子详情, postId={}", postId);
        ForumPostVO forumPostVO = forumPostService.getPostDetailById(postId);
        log.info("查询帖子详情controller层成功{}", forumPostVO);
        return Result.success(forumPostVO);
    }

    @PutMapping("/addPost")
    public Result addPost(@RequestBody ForumPostDTO forumPostDTO) {
        log.info("添加帖子controller层{}", forumPostDTO);
        forumPostService.addPost(forumPostDTO);
        log.info("添加帖子controller层成功");
        return Result.success();
    }
    @PutMapping("/addComment")
    public Result addComment(@RequestBody ForumCommentDTO forumCommentDTO) {
        log.info("添加评论controller层{}", forumCommentDTO);
        forumPostService.addComment(forumCommentDTO);
        log.info("添加评论controller层成功");
        return Result.success();
    }
    @DeleteMapping("/deletePost")
    public Result deletePost(@RequestParam Long postId) {
        log.info("删除帖子controller层postId={}", postId);
        forumPostService.deletePost(postId);
        log.info("删除帖子controller层成功");
        return Result.success();
    }
    @DeleteMapping("/deleteComment")
    public Result deleteComment(@RequestParam Long commentId) {
        log.info("controller:开始删除评论commentId={}", commentId);
        forumPostService.deleteComment(commentId);
        log.info("controller:删除评论成功");
        return Result.success();
    }

}
