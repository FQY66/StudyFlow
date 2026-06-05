package com.sf.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sf.dto.ResearchNewsPageQueryDTO;
import com.sf.entity.ResearchNews;
import com.sf.mapper.ResearchNewsMapper;
import com.sf.result.PageResult;
import com.sf.service.RagIngestService;
import com.sf.service.ResearchNewsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class ResearchNewsServiceImpl implements ResearchNewsService {
    @Autowired
    private ResearchNewsMapper researchNewsMapper;
    @Autowired
    private RagIngestService ragIngestService;

    @Override
    public PageResult pageQuery(ResearchNewsPageQueryDTO queryDTO) {
        PageHelper.startPage(queryDTO.getPage(), queryDTO.getPageSize());
        Page<ResearchNews> page = researchNewsMapper.pageQuery(queryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    @Override
    public ResearchNews getById(Integer id) {
        return researchNewsMapper.getById(id);
    }

    @Override
    public void insert(ResearchNews researchNews) {
        researchNews.setStatus("待审核");
        researchNews.setClickCount(researchNews.getClickCount() == null ? 0 : researchNews.getClickCount());
        researchNews.setSortOrder(researchNews.getSortOrder() == null ? 0 : researchNews.getSortOrder());

        researchNewsMapper.insert(researchNews);
    }

    @Override
    public void approve(Integer id) {
        ResearchNews researchNews = new ResearchNews();
        researchNews.setId(id);
        researchNews.setStatus("已发布");
        researchNewsMapper.update(researchNews);

        // 审核通过后，异步摄入RAG知识库
        CompletableFuture.runAsync(() -> {
            try {
                ResearchNews news = researchNewsMapper.getById(id);
                if (news != null) {
                    ragIngestService.ingestNews(
                        news.getTitle(),
                        news.getContent(),
                        news.getSource(),
                        news.getId()
                    );
                }
            } catch (Exception e) {
                log.error("[RAG] 资讯摄入失败: id={}, error={}", id, e.getMessage());
            }
        });
    }

    @Override
    public void update(ResearchNews researchNews) {
        researchNewsMapper.update(researchNews);
    }

    @Override
    public void delete(Integer id) {
        // 先获取资讯信息用于 RAG 删除
        ResearchNews news = null;
        try {
            news = researchNewsMapper.getById(id);
        } catch (Exception e) {
            log.warn("[RAG] 删除前查询资讯失败: id={}", id);
        }
        researchNewsMapper.delete(id);
        // 异步清理 RAG 知识库
        final ResearchNews finalNews = news;
        if (finalNews != null) {
            CompletableFuture.runAsync(() -> {
                try {
                    ragIngestService.deleteNews(finalNews.getTitle());
                } catch (Exception e) {
                    log.error("[RAG] 资讯删除失败: id={}, error={}", id, e.getMessage());
                }
            });
        }
    }

    @Override
    public void increaseClickCount(Integer id) {
        researchNewsMapper.increaseClickCount(id);
    }
}