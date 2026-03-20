package com.sf.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sf.dto.ForumPageQueryDTO;
import com.sf.entity.ForumPost;
import com.sf.mapper.ForumMapper;
import com.sf.result.PageResult;
import com.sf.service.ForumPostService;

public class ForumPostServiceImpl implements ForumPostService {
    @Autowired
    private ForumMapper forumMapper;
    @Override
    public PageResult pageQuery(ForumPageQueryDTO forumPageQueryDTO) {
        PageHelper.startPage(forumPageQueryDTO.getPage(), forumPageQueryDTO.getPageSize());
        Page<ForumPost> page = forumMapper.pageQuery(forumPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }
}
