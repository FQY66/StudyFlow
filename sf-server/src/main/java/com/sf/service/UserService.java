package com.sf.service;

import com.sf.dto.UserDTO;
import com.sf.result.PageResult;
import com.sf.dto.UserLoginDTO;
import com.sf.dto.UserPageQueryDTO;
import com.sf.vo.UserVO;
import com.sf.entity.User;

import java.util.List;

public interface UserService {
    UserVO login(UserLoginDTO userLoginDTO);

    void register(UserLoginDTO userLoginDTO);


    PageResult pageQuery(UserPageQueryDTO userPageQueryDTO);

    void update(UserDTO userDTO);

    void delete(int id);


}
