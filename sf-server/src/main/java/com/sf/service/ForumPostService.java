package com.sf.service;

import com.sf.dto.ForumCommentDTO;
import com.sf.dto.ForumPageQueryDTO;
import com.sf.dto.ForumPostDTO;
import com.sf.result.PageResult;
import com.sf.vo.ForumPostVO;

public interface ForumPostService {
    PageResult pageQuery(ForumPageQueryDTO forumPageQueryDTO);


    ForumPostVO getPostDetailById(Long postId);

    void addPost(ForumPostDTO forumPostVO);

    void addComment(ForumCommentDTO forumCommentDTO);
}
