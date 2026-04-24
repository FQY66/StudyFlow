package com.sf.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sf.dto.PremiumCoursePageQueryDTO;
import com.sf.entity.PremiumCourse;
import com.sf.mapper.PremiumCourseMapper;
import com.sf.result.PageResult;
import com.sf.service.PremiumCourseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class PremiumCourseServiceImpl implements PremiumCourseService {
    @Autowired
    private PremiumCourseMapper premiumCourseMapper;

    @Override
    public PageResult pageQuery(PremiumCoursePageQueryDTO queryDTO) {
        PageHelper.startPage(queryDTO.getPage(), queryDTO.getPageSize());
        Page<PremiumCourse> page = premiumCourseMapper.pageQuery(queryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    @Override
    public PremiumCourse getById(Integer id) {
        return premiumCourseMapper.getById(id);
    }

    @Override
    public void insert(PremiumCourse premiumCourse) {
        premiumCourse.setStatus("待审核");
        premiumCourse.setClickCount(premiumCourse.getClickCount() == null ? 0 : premiumCourse.getClickCount());
        premiumCourse.setSortOrder(premiumCourse.getSortOrder() == null ? 0 : premiumCourse.getSortOrder());
        premiumCourseMapper.insert(premiumCourse);
    }

    @Override
    public void approve(Integer id) {
        PremiumCourse premiumCourse = new PremiumCourse();
        premiumCourse.setId(id);
        premiumCourse.setStatus("已发布");
        premiumCourseMapper.update(premiumCourse);
    }

    @Override
    public void update(PremiumCourse premiumCourse) {
        premiumCourseMapper.update(premiumCourse);
    }

    @Override
    public void delete(Integer id) {
        premiumCourseMapper.delete(id);
    }
}