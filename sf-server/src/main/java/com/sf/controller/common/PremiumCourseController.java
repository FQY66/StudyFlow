package com.sf.controller.common;

import com.sf.dto.PremiumCoursePageQueryDTO;
import com.sf.entity.PremiumCourse;
import com.sf.result.PageResult;
import com.sf.result.Result;
import com.sf.service.PremiumCourseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/premiumCourse")
@Slf4j
public class PremiumCourseController {
    @Autowired
    private PremiumCourseService premiumCourseService;

    @GetMapping("/page")
    public Result<PageResult> page(PremiumCoursePageQueryDTO queryDTO) {
        return Result.success(premiumCourseService.pageQuery(queryDTO));
    }

    @GetMapping("/detail")
    public Result<PremiumCourse> detail(@RequestParam Integer id) {
        return Result.success(premiumCourseService.getById(id));
    }

    @PostMapping("/save")
    public Result save(@RequestBody PremiumCourse premiumCourse) {
        premiumCourseService.insert(premiumCourse);
        return Result.success();
    }

    @PutMapping("/approve")
    public Result approve(@RequestParam Integer id) {
        premiumCourseService.approve(id);
        return Result.success();
    }

    @PutMapping("/update")
    public Result update(@RequestBody PremiumCourse premiumCourse) {
        premiumCourseService.update(premiumCourse);
        return Result.success();
    }

    @DeleteMapping("/delete")
    public Result delete(@RequestParam Integer id) {
        premiumCourseService.delete(id);
        return Result.success();
    }
}