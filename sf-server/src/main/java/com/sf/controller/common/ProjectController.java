package com.sf.controller.common;

import com.sf.dto.ProjectPageQueryDTO;
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

    @PostMapping("/save")
    public Result save(@RequestBody ProjectStudy projectStudy) {
        log.info("新增项目Controller层: {}", projectStudy);
        projectService.insert(projectStudy);
        return Result.success();
    }

    @PutMapping("/update")
    public Result update(ProjectStudy projectStudy) {
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
}
