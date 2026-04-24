package com.sf.aspect;

import com.sf.annotation.AutoFill;
import com.sf.constant.AutoFillConstant;
import com.sf.context.BaseContext;
import com.sf.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Aspect
@Component
@Slf4j
public class AutoFillAspect {

    @Pointcut("execution(* com.sf.mapper.*.*(..))&& @annotation(com.sf.annotation.AutoFill)")
    public void autoFillPointcut() {
    }

    @Before("autoFillPointcut()")
    public void autoFill(JoinPoint joinPoint) {
        log.info("自动填充公共字段已被拦截....");
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class);
        OperationType operationType = autoFill.value();
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return;
        }
        Object entity = args[0];

        LocalDateTime now = LocalDateTime.now();
        String nowText = now.toString();
        Long currentId = BaseContext.getCurrentId();

        if (operationType == OperationType.INSERT) {
            try {
                invokeSetter(entity, AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class, now);
                invokeSetter(entity, AutoFillConstant.SET_CREATE_TIME, String.class, nowText);
                invokeSetter(entity, AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class, now);
                invokeSetter(entity, AutoFillConstant.SET_UPDATE_TIME, String.class, nowText);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (operationType == OperationType.UPDATE) {
            try {
                invokeSetter(entity, AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class, now);
                invokeSetter(entity, AutoFillConstant.SET_UPDATE_TIME, String.class, nowText);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void invokeSetter(Object entity, String methodName, Class<?> paramType, Object value) {
        try {
            Method method = entity.getClass().getDeclaredMethod(methodName, paramType);
            method.invoke(entity, value);
        } catch (NoSuchMethodException e) {
            log.warn("实体类{}没有{}({})方法，跳过该字段的填充", entity.getClass().getName(), methodName, paramType.getSimpleName());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

