package com.sf.service;

public interface OnlineStatusService {
    void markOnline(Long userId);

    void markOffline(Long userId);

    boolean isOnline(Long userId);

    long countOnlineUsers();
}
