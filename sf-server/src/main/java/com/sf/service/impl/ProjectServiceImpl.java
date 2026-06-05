package com.sf.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sf.dto.ProjectPageQueryDTO;
import com.sf.entity.ProjectSignup;
import com.sf.entity.ProjectStudy;
import com.sf.mapper.ProjectMapper;
import com.sf.mapper.UserMapper;
import com.sf.result.PageResult;
import com.sf.service.ProjectService;
import com.sf.service.RagIngestService;
import com.sf.vo.ProjectSignupUserVO;
import com.sf.vo.ProjectStudyVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class ProjectServiceImpl implements ProjectService {
    @Autowired
    private ProjectMapper projectMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private RagIngestService ragIngestService;

    @Override
    public PageResult pageQuery(ProjectPageQueryDTO projectPageQueryDTO) {
        log.info("分页查询项目Service层: {}", projectPageQueryDTO);
        PageHelper.startPage(projectPageQueryDTO.getPage(), projectPageQueryDTO.getPageSize());
        Page<ProjectStudyVO> page = projectMapper.pageQuery(projectPageQueryDTO);
        log.info("page的total: {}", page.getTotal());
        log.info("page的pageSize: {}", page.getPageSize());
        log.info("page的records: {}", page.getResult().size());
        List<ProjectStudyVO> records = page.getResult();
        records.forEach(record -> {
            record.setProjectSignupList(projectMapper.getSignupByProjectId(record.getId()));
        });
        return new PageResult(page.getTotal(), records);
    }

    @Override
    public PageResult pageQueryManage(ProjectPageQueryDTO projectPageQueryDTO) {
        log.info("管理端分页查询项目Service层: {}", projectPageQueryDTO);
        PageHelper.startPage(projectPageQueryDTO.getPage(), projectPageQueryDTO.getPageSize());
        Page<ProjectStudyVO> page = projectMapper.pageQueryManage(projectPageQueryDTO);
        List<ProjectStudyVO> records = page.getResult();
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
        // 设置项目状态为待审核
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

        // 审核通过后，异步摄入RAG知识库
        CompletableFuture.runAsync(() -> {
            try {
                ProjectStudyVO vo = projectMapper.getById(id);
                if (vo != null) {
                    ragIngestService.ingestProject(
                        vo.getTheme(),
                        vo.getIntroduction(),
                        vo.getContent(),
                        vo.getCategory(),
                        vo.getId()
                    );
                }
            } catch (Exception e) {
                log.error("[RAG] 项目摄入失败: id={}, error={}", id, e.getMessage());
            }
        });
    }

    @Override
    public void delete(Integer id) {
        log.info("删除项目Service层: {}", id);
        // 先获取项目信息用于 RAG 删除
        ProjectStudyVO vo = null;
        try {
            vo = projectMapper.getById(id);
        } catch (Exception e) {
            log.warn("[RAG] 删除前查询项目失败: id={}", id);
        }
        projectMapper.delete(id);
        // 异步清理 RAG 知识库
        final ProjectStudyVO finalVo = vo;
        if (finalVo != null) {
            CompletableFuture.runAsync(() -> {
                try {
                    ragIngestService.deleteProject(finalVo.getTheme(), finalVo.getCategory());
                } catch (Exception e) {
                    log.error("[RAG] 项目删除失败: id={}, error={}", id, e.getMessage());
                }
            });
        }
    }

    @Override
    public void signup(Integer projectId, Integer userId) {
        log.info("报名项目Service层: projectId={}, userId={}", projectId, userId);
        ProjectStudyVO project = projectMapper.getById(projectId);
        if (project == null) {
            throw new IllegalArgumentException("项目不存在");
        }
        // 检查是否已报名（防止重复报名）
        List<ProjectSignupUserVO> existingSignups = projectMapper.getSignupByProjectId(projectId);
        boolean alreadySignedUp = existingSignups != null && existingSignups.stream()
                .anyMatch(s -> s.getUserId().equals(userId));
        if (alreadySignedUp) {
            throw new IllegalArgumentException("你已经报名过该项目了");
        }
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

    @Override
    public void increaseClickCount(Integer id) {
        projectMapper.increaseClickCount(id);
    }

    @Override
    public void increaseLikeCount(Integer id) {
        projectMapper.increaseLikeCount(id);
    }

    @Override
    public List<ProjectStudyVO> getSignedUpProjectsByUserId(Integer userId) {
        return projectMapper.getSignedUpProjectsByUserId(userId);
    }

    @Override
    public Integer countAllProjects() {
        return projectMapper.countAllProjects();
    }

    @Override
    public Integer countTotalClick() {
        return projectMapper.countTotalClick();
    }

    @Override
    public Integer countTotalParticipants() {
        return projectMapper.countTotalParticipants();
    }
}
