package entity;

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
    private Long id;
    private String username;
    private String password;
    private String sex;
    private String email;
    private String phone;
    private String avatar;
    private String role;
    private Integer status;

    //@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

}
