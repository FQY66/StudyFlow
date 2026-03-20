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
public class User implements Serializable{
    /** 主键ID */
    private Long id;
    /** 用户名 */
    private String username;
    /** 密码（建议存储加密后的密文） */
    private String password;
    /** 姓名 */
    private String name;
    /** 性别 */
    private String sex;
    /** 邮箱 */
    private String email;
    /** 手机号 */
    private String phone;
    /** 头像URL */
    private String avatar;
    /** 角色（STUDENT/TEACHER/ADMIN） */
    private String role;
    /** 账号状态（1启用，0禁用） */
    private Integer status;

    //@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 更新时间 */
    private LocalDateTime updateTime;

}
