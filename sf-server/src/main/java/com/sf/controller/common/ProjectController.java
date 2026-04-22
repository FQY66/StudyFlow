package com.sf.controller.common;

import com.sf.dto.ProjectPageQueryDTO;
import com.sf.entity.ProjectSignup;
import com.sf.entity.ProjectStudy;
import com.sf.result.PageResult;
import com.sf.result.Result;
import com.sf.service.ProjectService;
import com.sf.vo.ProjectStudyVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/project")
@Slf4j
public class ProjectController {
    @Autowired
    private ProjectService projectService;

    @GetMapping("/page")
    public Result<PageResult> page(ProjectPageQueryDTO projectPageQueryDTO) {
        log.info("分页查询项目Controller层: {}", projectPageQueryDTO);
        PageResult pageResult = projectService.pageQuery(projectPageQueryDTO);
        return Result.success(pageResult);
    }

    @GetMapping("/detail")
    public Result<ProjectStudyVO> getById(@RequestParam Integer id) {
        log.info("根据id查询项目Controller层: {}", id);
        ProjectStudyVO project = projectService.getById(id);
        return Result.success(project);
    }

    @GetMapping("/categories")
    public Result<java.util.List<String>> categories() {
        log.info("查询项目类别列表Controller层");
        return Result.success(projectService.getCategories());
    }

    @PostMapping("/save")
    public Result save(@RequestBody ProjectStudy projectStudy) {
        log.info("新增项目Controller层: {}", projectStudy);
        projectService.insert(projectStudy);
        return Result.success();
    }

    @PutMapping("/update")
    public Result update(@RequestBody ProjectStudy projectStudy) {
        log.info("更新项目Controller层: {}", projectStudy);
        projectService.update(projectStudy);
        return Result.success();
    }

    @DeleteMapping("/delete")
    public Result delete(@RequestParam Integer id) {
        log.info("删除项目Controller层: {}", id);
        projectService.delete(id);
        return Result.success();
    }

    @PostMapping("/signup")
    public Result signup(@RequestParam Integer projectId, @RequestParam Integer userId) {
        log.info("报名项目Controller层: projectId={}, userId={}", projectId, userId);
        projectService.signup(projectId, userId);
        return Result.success();
    }
    @PostMapping("/approveSignup")
    public Result approveSignup(@RequestParam Integer projectId, @RequestParam Integer userId) {
        log.info("审核通过报名Controller层: projectId={}, userId={}", projectId, userId);
        projectService.approveSignup(projectId, userId);
        return Result.success();
    }

    @DeleteMapping("/cancelSignup")
    public Result cancelSignup(@RequestParam Integer projectId, @RequestParam Integer userId) {
        log.info("取消报名项目Controller层: projectId={}, userId={}", projectId, userId);
        projectService.cancelSignup(projectId, userId);
        return Result.success();
    }
}
