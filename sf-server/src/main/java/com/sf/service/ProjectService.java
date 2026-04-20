package com.sf.service;

import com.sf.dto.ProjectPageQueryDTO;
import com.sf.result.PageResult;
import com.sf.vo.ProjectStudyVO;
import org.springframework.stereotype.Service;

@Service
public interface ProjectService {
    PageResult pageQuery(ProjectPageQueryDTO projectPageQueryDTO);

    ProjectStudyVO getById(Integer id);
}
