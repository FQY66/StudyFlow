package com.sf.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PremiumCourse implements Serializable {
    private Integer id;
    private String title;
    private String category;
    private String summary;
    private String content;
    private String coverPath;
    private String videoPath;
    private String teacherName;
    private String duration;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer sortOrder;
    private Integer clickCount;
}