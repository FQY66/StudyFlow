package com.sf.mapper;

import com.github.pagehelper.Page;
import com.sf.annotation.AutoFill;
import com.sf.dto.PremiumCoursePageQueryDTO;
import com.sf.entity.PremiumCourse;
import com.sf.enumeration.OperationType;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PremiumCourseMapper {
    Page<PremiumCourse> pageQuery(PremiumCoursePageQueryDTO queryDTO);

    PremiumCourse getById(Integer id);

    @AutoFill(OperationType.INSERT)
    void insert(PremiumCourse premiumCourse);

    @AutoFill(OperationType.UPDATE)
    void update(PremiumCourse premiumCourse);

    void delete(Integer id);
}