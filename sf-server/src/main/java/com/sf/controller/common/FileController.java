package com.sf.controller.common;

import com.sf.result.Result;
import com.sf.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/common/file")
@Slf4j
public class FileController {

    @Autowired
    private FileService fileService;

    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) {
        log.info("文件上传开始, fileName={}, size={}",
                file == null ? null : file.getOriginalFilename(),
                file == null ? null : file.getSize());

        Map<String, Object> data = fileService.upload(file);

        log.info("文件上传成功, url={}", data.get("url"));
        return Result.success(data);
    }
}
