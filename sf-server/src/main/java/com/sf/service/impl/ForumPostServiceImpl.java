package com.sf.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sf.dto.ForumCommentDTO;
import com.sf.dto.ForumPageQueryDTO;
import com.sf.dto.ForumPostDTO;
import com.sf.entity.ForumComment;
import com.sf.entity.ForumPost;
import com.sf.mapper.ForumMapper;
import com.sf.mapper.UserMapper;
import com.sf.result.PageResult;
import com.sf.service.ForumPostService;
import com.sf.vo.ForumCommentVO;
import com.sf.vo.ForumPostVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

@Service
@Slf4j
public class ForumPostServiceImpl implements ForumPostService {

    @Autowired
    private ForumMapper forumMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public PageResult pageQuery(ForumPageQueryDTO forumPageQueryDTO) {
        PageHelper.startPage(forumPageQueryDTO.getPage(), forumPageQueryDTO.getPageSize());
        Page<ForumPost> page = forumMapper.pageQuery(forumPageQueryDTO);
        List<ForumPost> posts = page.getResult();
        List<ForumPostVO> forumPostVOS = new ArrayList<>();
        for (ForumPost post : posts) {
            forumPostVOS.add(getPostDetailById(post.getId()));
        }
        return new PageResult(page.getTotal(), forumPostVOS);
    }

    @Override
    public ForumPostVO getPostDetailById(Long postId) {
        ForumPostVO forumPostVO = forumMapper.getPostById(postId);
        if (forumPostVO == null) {
            return null;
        }

        List<ForumCommentVO> comments = forumMapper.listCommentsByPostId(postId);
        forumPostVO.setComments(comments);
        forumPostVO.setCommentCount(comments == null ? 0 : comments.size());
        return forumPostVO;
    }

    @Override
    public void addPost(ForumPostDTO forumPostDTO) {
        ForumPost forumPost = new ForumPost();
        BeanUtils.copyProperties(forumPostDTO, forumPost);
        forumPost.setAuthorId(userMapper.getById(forumPostDTO.getAuthorName()));
        log.info("forumPost: {}", forumPost);
        forumMapper.addPost(forumPost);
    }

    @Override
    public void addComment(ForumCommentDTO forumCommentDTO) {
        ForumComment forumComment = new ForumComment();
        BeanUtils.copyProperties(forumCommentDTO, forumComment);
        forumComment.setAuthorId(userMapper.getById(forumCommentDTO.getAuthorName()));
        forumMapper.addComment(forumComment);
    }

    @Override
    public void deletePost(Long postId) {
        forumMapper.deletePost(postId);
        List<Long> commentIds = forumMapper.getCommentIdsBypostId(postId);
        if (commentIds != null && !commentIds.isEmpty()) {
            //对commentIds里面的每个元素调用deleteComment方法删除，
            // 该方法等价 commentIds.forEach(commentId -> forumMapper.deleteComment(commentId));
            log.info("Mapper:删除评论，commentIds: {}", commentIds);
            forumMapper.deleteByCommentIds(commentIds);
        }
    }

    @Override
    public void deleteComment(Long commentId) {
        forumMapper.deleteComment(commentId);
    }

}
