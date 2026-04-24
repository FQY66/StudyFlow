package com.sf.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sf.dto.ProjectPageQueryDTO;
import com.sf.entity.ProjectSignup;
import com.sf.entity.ProjectStudy;
import com.sf.mapper.ProjectMapper;
import com.sf.result.PageResult;
import com.sf.service.ProjectService;
import com.sf.vo.ProjectStudyVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class ProjectServiceImpl implements ProjectService {
    @Autowired
    private ProjectMapper projectMapper;
    @Override
    public PageResult pageQuery(ProjectPageQueryDTO projectPageQueryDTO) {
        log.info("分页查询项目Service层: {}", projectPageQueryDTO);
        PageHelper.startPage(projectPageQueryDTO.getPage(), projectPageQueryDTO.getPageSize());
        Page<ProjectStudyVO> page = projectMapper.pageQuery(projectPageQueryDTO);
        log.info("page的total: {}", page.getTotal());
        log.info("page的pageSize: {}", page.getPageSize());
        log.info("page的records: {}", page.getResult().size());
        List<ProjectStudyVO> records = page.getResult();
        // 填充报名用户列表
        records.forEach(record -> {
            record.setProjectSignupList(projectMapper.getSignupByProjectId(record.getId()));
        });
        return new PageResult(page.getTotal(), records);
    }

    @Override
    public List<String> getCategories() {
        return projectMapper.getCategories();
    }

    @Override
    public ProjectStudyVO getById(Integer id) {
        log.info("根据id查询项目Service层: {}", id);
        ProjectStudyVO project = projectMapper.getById(id);
        if (project != null) {
            project.setProjectSignupList(projectMapper.getSignupByProjectId(project.getId()));
        }
        return project;
    }

    @Override
    public void insert(ProjectStudy projectStudy) {
        log.info("新增项目Service层: {}", projectStudy);
        //设置项目状态为待审核
        projectStudy.setStatus("待审核");
        projectStudy.setLikeCount(0);
        projectStudy.setClickCount(0);
        projectMapper.insert(projectStudy);
    }

    @Override
    public void update(ProjectStudy projectStudy) {
        log.info("更新项目Service层: {}", projectStudy);
        projectMapper.update(projectStudy);
    }

    @Override
    public void approve(Integer id) {
        log.info("审核通过项目Service层: {}", id);
        ProjectStudy projectStudy = new ProjectStudy();
        projectStudy.setId(id);
        projectStudy.setStatus("已发布");
        projectMapper.update(projectStudy);
    }

    @Override
    public void delete(Integer id) {
        log.info("删除项目Service层: {}", id);
        projectMapper.delete(id);
    }

    @Override
    public void signup(Integer projectId, Integer userId) {
        log.info("报名项目Service层: projectId={}, userId={}", projectId, userId);
        ProjectSignup projectSignup = new ProjectSignup();
        projectSignup.setProjectId(Long.valueOf(projectId));
        projectSignup.setUserId(Long.valueOf(userId));
        projectSignup.setStatus("待审核");
        projectMapper.signup(projectSignup);
    }

    @Override
    public void approveSignup(Integer projectId, Integer userId) {
        log.info("审核通过报名Service层: projectId={}, userId={}", projectId, userId);
        projectMapper.approveSignup(projectId, userId);
    }

    @Override
    public void cancelSignup(Integer projectId, Integer userId) {
        log.info("取消报名项目Service层: projectId={}, userId={}", projectId, userId);
        projectMapper.cancelSignup(projectId, userId);
    }
}
