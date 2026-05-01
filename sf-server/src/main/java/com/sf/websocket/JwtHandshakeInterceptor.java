package com.sf.websocket;

import com.sf.constant.JwtClaimsConstant;
import com.sf.properties.JwtProperties;
import com.sf.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@Slf4j
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    @Autowired
    private JwtProperties jwtProperties;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        URI uri = request.getURI();
        String token = extractToken(uri);
        if (token != null && !token.isEmpty()) {
            try {
                Claims claims = JwtUtil.parseJWT(jwtProperties.getAdminSecretKey(), token);
                Long userId = Long.valueOf(claims.get(JwtClaimsConstant.USER_ID).toString());
                attributes.put("userId", userId);
                log.info("WebSocket handshake success by token: userId={}, uri={}", userId, uri);
                return true;
            } catch (Exception e) {
                log.warn("WebSocket token invalid, uri={}, reason={}", uri, e.getMessage());
            }
        }

        Long pathUserId = extractPathUserId(uri);
        if (pathUserId != null && pathUserId > 0) {
            attributes.put("userId", pathUserId);
            log.info("WebSocket handshake success by path: userId={}, uri={}", pathUserId, uri);
            return true;
        }

        log.warn("WebSocket handshake rejected: user id missing, uri={}", uri);
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
    }

    private String extractToken(URI uri) {
        String query = uri.getQuery();
        if (query == null || query.isEmpty()) {
            return null;
        }
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && "token".equals(kv[0])) {
                return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private Long extractPathUserId(URI uri) {
        String path = uri.getPath();
        if (path == null || path.isEmpty()) {
            return null;
        }
        String[] parts = path.split("/");
        if (parts.length == 0) {
            return null;
        }
        String last = parts[parts.length - 1];
        if (last == null || last.isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(last);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
