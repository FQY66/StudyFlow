package com.sf.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ForumPageQueryDTO extends PageQueryDTO implements Serializable {

    // 发帖人ID
    private Long userId;
    // 发帖人姓名
    private String userName;

    // 帖子标题
    private String title;
    // 帖子分类
    private String category;
}
