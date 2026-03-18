package vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
// 管理员登录VO
public class UserLoginVO implements Serializable {
    // 管理员ID
    private Long id;
    // 管理员登录令牌
    private String token;
    // 管理员用户名
    private String username;
    // 管理员姓名
    private String name;
    // 管理员角色
    private String role;
    // 管理员头像
    private String avatar;
    //邮箱地址
    private String emali;
    //手机号
    private String phone;
    //性别
    private String sex;
}
