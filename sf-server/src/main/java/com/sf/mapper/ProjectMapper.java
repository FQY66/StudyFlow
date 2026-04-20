package com.sf.mapper;

import com.github.pagehelper.Page;
import com.sf.dto.ProjectPageQueryDTO;
import com.sf.entity.ProjectStudy;
import com.sf.vo.ProjectStudyVO;
import com.sf.vo.UserVO;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface ProjectMapper {

    Page<ProjectStudyVO> pageQuery(ProjectPageQueryDTO projectPageQueryDTO);

    List<UserVO> getSignupByProjectId(Integer projectId);

    void insert(ProjectStudy projectStudy);

    ProjectStudyVO getById(Integer id);
}