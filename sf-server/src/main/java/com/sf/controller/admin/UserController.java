package com.sf.controller.admin;

import com.sf.result.Result;
import com.sf.service.UserService;
import dto.UserLoginDTO;
import entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vo.UserLoginVO;

@RestController
@RequestMapping("/admin")
@Slf4j

public class UserController {

    @Autowired
    private UserService userService;
    @PostMapping("/login")
    public Result<UserLoginVO> login(@RequestBody UserLoginDTO userLoginDTO) {
        log.info("管理员登录: {}", userLoginDTO);
        UserLoginVO userLoginVO = userService.login(userLoginDTO);
        return Result.success(userLoginVO);
    }

    @PostMapping("/register")
    public Result register(@RequestBody UserLoginDTO userLoginDTO){
        log.info("管理员注册: {}", userLoginDTO);
        userService.register(userLoginDTO);
        return Result.success();
    }

    @GetMapping("/check")
    public Result checkToken() {
        // 走到这里说明 JWT 拦截器校验通过
        return Result.success();
    }
}
