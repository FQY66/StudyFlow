package com.sf.vo;

import com.sf.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProjectStudyVO {
    //主键id
    private Integer id;
    //封面路径
    private String coverPath;
    //项目主题
    private String theme;
    //项目简介
    private String introduction;
    //项目内容
    private String content;
    //项目类别
    private String category;
    //报名上限
    private Integer capacity;

    //项目状态，用于审核员审核
    private String status;

    //创建时间
    private String createTime;
    //更新时间
    private String updateTime;

    //点赞数
    private Integer likeCount;
    //点击量
    private Integer clickCount;

    //报名用户列表
    private List<UserVO> projectSignupList;
}
