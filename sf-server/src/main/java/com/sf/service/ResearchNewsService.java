package com.sf.service;

import com.sf.dto.ResearchNewsPageQueryDTO;
import com.sf.entity.ResearchNews;
import com.sf.result.PageResult;

public interface ResearchNewsService {
    PageResult pageQuery(ResearchNewsPageQueryDTO queryDTO);

    ResearchNews getById(Integer id);

    void insert(ResearchNews researchNews);

    void approve(Integer id);

    void update(ResearchNews researchNews);

    void delete(Integer id);
}