package com.sf.service.impl;

import com.sf.constant.ResourceConstant;
import com.sf.service.FileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class FileServiceImpl implements FileService {

    @Value("${sf.upload.local-path:uploads}")
    private String uploadRootPath;

//    @Value("${sf.upload.url-prefix:/uploads}")
//    private String uploadUrlPrefix;

    @Override
    public Map<String, Object> upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = StringUtils.getFilenameExtension(originalFilename);
        String safeExtension = extension == null ? "" : "." + extension.toLowerCase();

        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        File targetDir = new File(uploadRootPath, datePath);
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            throw new RuntimeException("创建上传目录失败: " + targetDir.getAbsolutePath());
        }

        String storedName = UUID.randomUUID().toString().replace("-", "") + safeExtension;
        File targetFile = new File(targetDir, storedName);

        try {
            file.transferTo(targetFile);
        } catch (IOException e) {
            throw new RuntimeException("保存文件失败", e);
        }

        String relativePath = datePath + "/" + storedName;
        String url = ResourceConstant.DEFAULT_FILE_UPLOAD_PATH + relativePath;

        Map<String, Object> result = new HashMap<>();
        result.put("url", url.replace("\\", "/"));
        result.put("originalName", originalFilename);
        result.put("storedName", storedName);
        result.put("size", file.getSize());
        result.put("contentType", file.getContentType());
        return result;
    }
}
