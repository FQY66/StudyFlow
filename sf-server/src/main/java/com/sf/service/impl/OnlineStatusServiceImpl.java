package com.sf.service.impl;

import com.sf.service.OnlineStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class OnlineStatusServiceImpl implements OnlineStatusService {

    private static final String ONLINE_KEY_PREFIX = "chat:online:user:";
    private static final long ONLINE_TTL_MINUTES = 15L;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public void markOnline(Long userId) {
        if (userId == null) {
            return;
        }
        redisTemplate.opsForValue().set(key(userId), "1", ONLINE_TTL_MINUTES, TimeUnit.MINUTES);
    }

    @Override
    public void markOffline(Long userId) {
        if (userId == null) {
            return;
        }
        redisTemplate.delete(key(userId));
    }

    @Override
    public boolean isOnline(Long userId) {
        if (userId == null) {
            return false;
        }
        Boolean exists = redisTemplate.hasKey(key(userId));
        return Boolean.TRUE.equals(exists);
    }

    private String key(Long userId) {
        return ONLINE_KEY_PREFIX + userId;
    }
}
