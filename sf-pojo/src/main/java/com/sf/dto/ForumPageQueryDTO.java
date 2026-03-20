package com.sf.dto;

import java.io.Serializable;

public class ForumPageQueryDTO extends PageQueryDTO implements Serializable {

    // 发帖人ID
    private long userId;
    // 发帖人姓名
    private String userName;

    // 帖子标题
    private String title;
    // 帖子分类
    private String category;
}
