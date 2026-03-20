package com.sf.mapper;

import com.github.pagehelper.Page;
import com.sf.dto.ForumPageQueryDTO;
import com.sf.entity.ForumPost;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ForumMapper {
    Page<ForumPost> pageQuery(ForumPageQueryDTO forumPageQueryDTO);
}
