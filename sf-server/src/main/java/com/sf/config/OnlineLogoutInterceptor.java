package com.sf.config;

import com.sf.service.OnlineStatusService;
import com.sf.context.BaseContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OnlineLogoutInterceptor implements HandlerInterceptor {

    @Autowired
    private OnlineStatusService onlineStatusService;

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        String uri = request.getRequestURI();
        if (uri != null && uri.contains("/admin/logout")) {
            Long userId = BaseContext.getCurrentId();
            onlineStatusService.markOffline(userId);
        }
    }
}
