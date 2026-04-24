package com.sf.controller.common;

import com.sf.dto.ResearchNewsPageQueryDTO;
import com.sf.entity.ResearchNews;
import com.sf.result.PageResult;
import com.sf.result.Result;
import com.sf.service.ResearchNewsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/researchNews")
@Slf4j
public class ResearchNewsController {
    @Autowired
    private ResearchNewsService researchNewsService;

    @GetMapping("/page")
    public Result<PageResult> page(ResearchNewsPageQueryDTO queryDTO) {
        return Result.success(researchNewsService.pageQuery(queryDTO));
    }

    @GetMapping("/detail")
    public Result<ResearchNews> detail(@RequestParam Integer id) {
        return Result.success(researchNewsService.getById(id));
    }

    @PostMapping("/save")
    public Result save(@RequestBody ResearchNews researchNews) {
        log.info("新增研究新闻Controller层: {}", researchNews);
        researchNewsService.insert(researchNews);
        return Result.success();
    }

    @PutMapping("/approve")
    public Result approve(@RequestParam Integer id) {
        researchNewsService.approve(id);
        return Result.success();
    }

    @PutMapping("/update")
    public Result update(@RequestBody ResearchNews researchNews) {
        researchNewsService.update(researchNews);
        return Result.success();
    }

    @DeleteMapping("/delete")
    public Result delete(@RequestParam Integer id) {
        researchNewsService.delete(id);
        return Result.success();
    }
}