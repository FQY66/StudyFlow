package com.sf.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sf.constant.JwtClaimsConstant;
import com.sf.constant.MessageConstant;
import com.sf.constant.ResourceConstant;
import com.sf.constant.StatusConstant;
import com.sf.dto.UserDTO;
import com.sf.exception.AccountLockedException;
import com.sf.exception.AccountNotFoundException;
import com.sf.exception.PasswordErrorException;
import com.sf.mapper.UserMapper;
import com.sf.properties.JwtProperties;
import com.sf.result.PageResult;
import com.sf.service.UserService;
import com.sf.utils.JwtUtil;
import com.sf.dto.UserLoginDTO;
import com.sf.dto.UserPageQueryDTO;
import com.sf.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import com.sf.vo.UserVO;


import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private JwtProperties jwtProperties;

    @Override
    public UserVO login(UserLoginDTO userLoginDTO) {
        String username = userLoginDTO.getUsername();
        String password = userLoginDTO.getPassword();

        //1、根据用户名查询数据库中的数据
        User user = userMapper.getByUsername(username);

        //2、处理各种异常情况（用户名不存在、密码不对、账号被锁定）
        if (user == null) {
            //账号不存在
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        //密码比对
        // TODO 后期需要进行md5加密，然后再进行比对
        password = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!password.equals(user.getPassword())) {
            //密码错误
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        if (user.getStatus() == StatusConstant.DISABLE) {
            //账号被锁定
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }
        log.info("加密后的密码：" + password);

        //生成JWT密钥
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, user.getId());
        String token = JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                claims);
        log.info("生成的JWT令牌：" + token);
        //3、返回实体对象
        return UserVO.builder()
                .id(user.getId())
                .token(token)
                .username(username)
                .name(user.getName())
                .role(user.getRole())
                .avatar(user.getAvatar())
                .emali(user.getEmail())
                .phone(user.getPhone())
                .sex(user.getSex())
                .build();
    }

    @Override
    public void register(UserLoginDTO userLoginDTO) {
        //1、从VO中获取数据
        String username = userLoginDTO.getUsername();
        String password = userLoginDTO.getPassword();
        String name = "用户" + UUID.randomUUID().toString().substring(0, 8);
        //2、检查用户名是否已存在
        User existingUser = userMapper.getByUsername(username);
        if (existingUser != null) {
            //用户名已存在
            throw new IllegalArgumentException(MessageConstant.USERNAME_EXISTS);
        }
        //3、对密码进行MD5加密
        String MD5pwd = DigestUtils.md5DigestAsHex(password.getBytes());
        int randomInt = new Random().nextInt(10);
        String avatar = ResourceConstant.DEFAULT_AVATAR + "defaultAvatar" + randomInt + ".png";
        //4、创建新用户实体
//        User user = new User();
//        user.setUsername(username);
//        user.setPassword(password);
//        user.setName(name);
//        user.setStatus(StatusConstant.ENABLE); // 默认启用状态
        User user = User.builder()
                        .username(username)
                        .name(name)
                        .password(MD5pwd)
                        .status(StatusConstant.ENABLE)
                        .sex("男")
                        .email("12345678@qq.com")
                        .phone("13800000000")
                        .role(userLoginDTO.getRole())
                        .avatar(avatar)
                        .build();
        //5、插入数据库
        userMapper.insert(user);
    }

    @Override
    public PageResult pageQuery(UserPageQueryDTO userPageQueryDTO) {
        PageHelper.startPage(userPageQueryDTO.getPage(), userPageQueryDTO.getPageSize());
        Page<User> page = userMapper.pageQuery(userPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    @Override
    public void update(UserDTO userDTO) {
        User user = new User();
        BeanUtils.copyProperties(userDTO, user);
        // 密码加密
        if(user.getPassword()!=null){
            user.setPassword(DigestUtils.md5DigestAsHex(user.getPassword().getBytes()));
        }
        log.info("更新用户信息，当前用户：{}", user);
        userMapper.update(user);
    }

    @Override
    public void delete(int id) {
        userMapper.delete(id);
        log.info("删除用户: {}", id);
    }

}
