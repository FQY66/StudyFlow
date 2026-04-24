package com.sf.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PremiumCoursePageQueryDTO extends PageQueryDTO implements Serializable {
    private String keyword;
    private String category;
    private String status;
}