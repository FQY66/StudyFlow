package com.sf.controller.admin;

import com.sf.dto.UserDTO;
import com.sf.result.PageResult;
import com.sf.result.Result;
import com.sf.service.UserService;
import com.sf.dto.UserLoginDTO;
import com.sf.dto.UserPageQueryDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.sf.vo.UserVO;

@RestController
@RequestMapping("/admin")
@Slf4j

public class UserController {

    @Autowired
    private UserService userService;

    //账号登录
    @PostMapping("/login")
    public Result<UserVO> login(@RequestBody UserLoginDTO userLoginDTO) {
        log.info("角色登录: {}", userLoginDTO);
        UserVO userLoginVO = userService.login(userLoginDTO);
        return Result.success(userLoginVO);
    }

    // 账号注册
    @PostMapping("/register")
    public Result register(@RequestBody UserLoginDTO userLoginDTO){
        log.info("角色注册: {}", userLoginDTO);
        userService.register(userLoginDTO);
        return Result.success();
    }

    // token验证通过
    @GetMapping("/check")
    public Result checkToken() {
        return Result.success();
    }

    @GetMapping("/page")
    public Result<PageResult> page(UserPageQueryDTO userPageQueryDTO) {
        log.info("分页查询controller层{}", userPageQueryDTO);
        PageResult pageResult = userService.pageQuery(userPageQueryDTO);
        return Result.success(pageResult);
    }

    @PutMapping("/updateUser")
    public Result update(@RequestBody UserDTO userDTO){
        log.info("更新用户信息: {}", userDTO);
        userService.update(userDTO);
        return Result.success();
    }

    @DeleteMapping("/deleteUser")
    public Result delete(@RequestParam int id){
        log.info("删除用户: {}", id);
        userService.delete(id);
        return Result.success();
    }

}
