package com.sf.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sf.dto.ResearchNewsPageQueryDTO;
import com.sf.entity.ResearchNews;
import com.sf.mapper.ResearchNewsMapper;
import com.sf.result.PageResult;
import com.sf.service.ResearchNewsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class ResearchNewsServiceImpl implements ResearchNewsService {
    @Autowired
    private ResearchNewsMapper researchNewsMapper;

    @Override
    public PageResult pageQuery(ResearchNewsPageQueryDTO queryDTO) {
        PageHelper.startPage(queryDTO.getPage(), queryDTO.getPageSize());
        Page<ResearchNews> page = researchNewsMapper.pageQuery(queryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    @Override
    public ResearchNews getById(Integer id) {
        return researchNewsMapper.getById(id);
    }

    @Override
    public void insert(ResearchNews researchNews) {
        researchNews.setStatus("待审核");
        researchNews.setClickCount(researchNews.getClickCount() == null ? 0 : researchNews.getClickCount());
        researchNews.setSortOrder(researchNews.getSortOrder() == null ? 0 : researchNews.getSortOrder());

        researchNewsMapper.insert(researchNews);
    }

    @Override
    public void approve(Integer id) {
        ResearchNews researchNews = new ResearchNews();
        researchNews.setId(id);
        researchNews.setStatus("已发布");
        researchNewsMapper.update(researchNews);
    }

    @Override
    public void update(ResearchNews researchNews) {
        researchNewsMapper.update(researchNews);
    }

    @Override
    public void delete(Integer id) {
        researchNewsMapper.delete(id);
    }
}