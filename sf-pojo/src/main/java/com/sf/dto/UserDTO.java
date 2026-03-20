package com.sf.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class UserDTO implements Serializable {
    //用户名
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
    /** 账号状态（1启用，0禁用） */
    private Integer status;
}
