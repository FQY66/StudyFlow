package com.sf.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface FileService {
    Map<String, Object> upload(MultipartFile file);
}
