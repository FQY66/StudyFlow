package com.sf.controller.common;

import com.sf.dto.ForumPageQueryDTO;
import com.sf.result.PageResult;
import com.sf.result.Result;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.sf.service.ForumPostService;
@RestController
@RequestMapping("/Forum")
@Slf4j
public class ForumController {

    @Autowired
    private  ForumPostService forumPostService;
    /** 分页查询帖子 */
    @RequestMapping("/page")
    public Result<PageResult> page(@RequestBody ForumPageQueryDTO forumPageQueryDTO) {
        log.info("分页查询帖子controller层{}", forumPageQueryDTO);
        PageResult pageResult = forumPostService.pageQuery(forumPageQueryDTO);
        return Result.success(pageResult);
    }
}
