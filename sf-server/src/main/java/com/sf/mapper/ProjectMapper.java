package com.sf.mapper;

import com.github.pagehelper.Page;
import com.sf.annotation.AutoFill;
import com.sf.dto.ProjectPageQueryDTO;
import com.sf.entity.ProjectSignup;
import com.sf.entity.ProjectStudy;
import com.sf.enumeration.OperationType;
import com.sf.vo.ProjectSignupUserVO;
import com.sf.vo.ProjectStudyVO;
import com.sf.vo.UserVO;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface ProjectMapper {

    Page<ProjectStudyVO> pageQuery(ProjectPageQueryDTO projectPageQueryDTO);

    List<ProjectSignupUserVO> getSignupByProjectId(Integer projectId);

    List<String> getCategories();

    @AutoFill(OperationType.INSERT)
    void insert(ProjectStudy projectStudy);

    @AutoFill(OperationType.UPDATE)
    void update(ProjectStudy projectStudy);

    ProjectStudyVO getById(Integer id);

    void delete(Integer id);

    @AutoFill(OperationType.INSERT)
    void signup(ProjectSignup projectSignup);

    void approveSignup(Integer projectId, Integer userId);

    void cancelSignup(Integer projectId, Integer userId);
}