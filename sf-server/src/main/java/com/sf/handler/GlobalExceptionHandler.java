package com.sf.handler;

import com.sf.exception.BaseException;
import com.sf.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理器，处理项目中抛出的业务异常
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 捕获业务异常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(BaseException ex){
        log.error("异常信息：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    @ExceptionHandler
    public Result exceptionHandler(SQLIntegrityConstraintViolationException ex){
        String message = ex.getMessage();
        if (message.contains("Duplicate entry")) {
            // 处理用户名重复的情况
            String[] s = message.split(" ");
            return Result.error("用户名" + s[2] + "已存在");
        } else if (message.contains("foreign key constraint fails")) {
            // 处理外键约束错误
            log.error("关联的用户不存在：{}", message);
            return Result.error("关联的用户不存在");
        } else {
            // 其他数据库约束错误
            log.error("数据库操作失败：{}", message);
            return Result.error("数据库操作失败");
        }
    }
}
