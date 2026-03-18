package com.sf.service;

import com.sf.result.Result;
import dto.UserLoginDTO;
import entity.User;
import vo.UserLoginVO;


public interface UserService {
    UserLoginVO login(UserLoginDTO userLoginDTO);

    void register(UserLoginDTO userLoginDTO);
}
