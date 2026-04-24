package com.sf.service;

import com.sf.dto.PremiumCoursePageQueryDTO;
import com.sf.entity.PremiumCourse;
import com.sf.result.PageResult;

public interface PremiumCourseService {
    PageResult pageQuery(PremiumCoursePageQueryDTO queryDTO);

    PremiumCourse getById(Integer id);

    void insert(PremiumCourse premiumCourse);

    void approve(Integer id);

    void update(PremiumCourse premiumCourse);

    void delete(Integer id);
}