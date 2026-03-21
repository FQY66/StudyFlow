package com.sf.mapper;

import com.github.pagehelper.Page;
import com.sf.annotation.AutoFill;
import com.sf.dto.ForumPageQueryDTO;
import com.sf.dto.ForumPostDTO;
import com.sf.entity.ForumComment;
import com.sf.entity.ForumPost;
import com.sf.enumeration.OperationType;
import com.sf.vo.ForumCommentVO;
import com.sf.vo.ForumPostVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ForumMapper {
    Page<ForumPost> pageQuery(ForumPageQueryDTO forumPageQueryDTO);

    ForumPostVO getPostById(Long id);

    List<ForumCommentVO> listCommentsByPostId(Long postId);



    @AutoFill(OperationType.INSERT)
    void addPost(ForumPost forumPost);

    @AutoFill(OperationType.INSERT)
    void addComment(ForumComment forumComment);
}
