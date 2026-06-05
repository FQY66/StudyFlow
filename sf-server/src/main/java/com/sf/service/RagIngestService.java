package com.sf.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * RAG 知识库摄入服务 —— 调用 Python rag_api 的 /rag/ingest 接口
 */
@Service
@Slf4j
public class RagIngestService {

    @Value("${sf.rag.api-url:http://127.0.0.1:8000}")
    private String ragApiUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 摄入一篇文档到 RAG 知识库
     */
    public void ingest(String title, String content, String source, String url, String publishTime) {
        if (content == null || content.trim().isEmpty()) {
            log.info("[RAG] 跳过空内容: title={}", title);
            return;
        }
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("title", title);
            body.put("content", content.trim());
            body.put("source", source);
            body.put("url", url != null ? url : "");
            body.put("publish_time", publishTime != null ? publishTime : "");

            String json = objectMapper.writeValueAsString(body);
            HttpURLConnection conn = (HttpURLConnection) new URL(ragApiUrl + "/rag/ingest").openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(30000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                log.info("[RAG] 摄入成功: title={}, source={}, url={}", title, source, url);
            } else {
                log.warn("[RAG] 摄入失败, HTTP {}: title={}", code, title);
            }
            conn.disconnect();
        } catch (Exception e) {
            log.error("[RAG] 摄入异常: title={}, error={}", title, e.getMessage());
        }
    }

    /**
     * 摄入研学资讯
     */
    public void ingestNews(String title, String content, String source, Integer id) {
        String url = buildNewsUrl(id);
        ingest(title, content, "研学资讯", url, "");
    }

    /**
     * 摄入研学项目
     */
    public void ingestProject(String theme, String introduction, String content, String category, Integer id) {
        String title = theme != null ? theme : "未命名项目";
        String fullContent = (introduction != null ? introduction + "\n" : "") + (content != null ? content : "");
        String source = "研学项目";
        if (category != null && !category.isEmpty()) {
            source = "研学项目-" + category;
        }
        String url = buildProjectUrl(id);
        ingest(title, fullContent, source, url, "");
    }

    private String buildNewsUrl(Integer id) {
        // 前端路由：/student/projects/news/detail/researchNews/{id}
        return "/student/projects/news/detail/researchNews/" + (id != null ? id : 0);
    }

    private String buildProjectUrl(Integer id) {
        // 前端路由：/student/projects/detail/{id}
        return "/student/projects/detail/" + (id != null ? id : 0);
    }

    /**
     * 从 RAG 知识库删除文档
     */
    public void deleteDocument(String title, String source) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("title", title);
            body.put("source", source);

            String json = objectMapper.writeValueAsString(body);
            HttpURLConnection conn = (HttpURLConnection) new URL(ragApiUrl + "/rag/delete").openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                log.info("[RAG] 删除成功: title={}, source={}", title, source);
            } else {
                log.warn("[RAG] 删除失败, HTTP {}: title={}", code, title);
            }
            conn.disconnect();
        } catch (Exception e) {
            log.error("[RAG] 删除异常: title={}, error={}", title, e.getMessage());
        }
    }

    /**
     * 删除研学资讯的 RAG 文档
     */
    public void deleteNews(String title) {
        deleteDocument(title, "研学资讯");
    }

    /**
     * 删除研学项目的 RAG 文档（需要知道分类）
     */
    public void deleteProject(String theme, String category) {
        String title = theme != null ? theme : "未命名项目";
        String source = "研学项目";
        if (category != null && !category.isEmpty()) {
            source = "研学项目-" + category;
        }
        deleteDocument(title, source);
    }
}
