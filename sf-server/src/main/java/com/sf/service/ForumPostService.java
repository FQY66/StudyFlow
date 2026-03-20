package com.sf.service;

import com.sf.dto.ForumPageQueryDTO;
import com.sf.result.PageResult;

public interface ForumPostService {
    PageResult pageQuery(ForumPageQueryDTO forumPageQueryDTO);
}
