package com.sf.controller.common;

import com.sf.dto.ProjectPageQueryDTO;
import com.sf.result.PageResult;
import com.sf.result.Result;
import com.sf.service.ProjectService;
import com.sf.vo.ProjectStudyVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

//    @PutMapping("/signup")
//    public Result<Void> signup(ProjectStudyDTO projectStudyDTO) {
//        log.info("报名项目Controller层: {}", projectStudyDTO);
//        projectService.insert(projectStudyDTO);
//        return Result.success();
//    }
}