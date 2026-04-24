package com.sf.service;

import com.sf.dto.ProjectPageQueryDTO;
import com.sf.entity.ProjectStudy;
import com.sf.result.PageResult;
import com.sf.vo.ProjectStudyVO;
import org.springframework.stereotype.Service;

@Service
public interface ProjectService {
    PageResult pageQuery(ProjectPageQueryDTO projectPageQueryDTO);

    java.util.List<String> getCategories();

    ProjectStudyVO getById(Integer id);

    void insert(ProjectStudy projectStudy);

    void update(ProjectStudy projectStudy);

    void approve(Integer id);

    void delete(Integer id);

    void signup(Integer projectId, Integer userId);

    void approveSignup(Integer projectId, Integer userId);

    void cancelSignup(Integer projectId, Integer userId);
}
