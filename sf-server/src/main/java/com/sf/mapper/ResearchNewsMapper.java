package com.sf.mapper;

import com.github.pagehelper.Page;
import com.sf.annotation.AutoFill;
import com.sf.dto.ResearchNewsPageQueryDTO;
import com.sf.entity.ResearchNews;
import com.sf.enumeration.OperationType;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ResearchNewsMapper {
    Page<ResearchNews> pageQuery(ResearchNewsPageQueryDTO queryDTO);

    ResearchNews getById(Integer id);

    @AutoFill(OperationType.INSERT)
    void insert(ResearchNews researchNews);

    @AutoFill(OperationType.UPDATE)
    void update(ResearchNews researchNews);

    void delete(Integer id);
}