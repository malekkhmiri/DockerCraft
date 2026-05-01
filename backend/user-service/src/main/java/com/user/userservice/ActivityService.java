package com.user.userservice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityService {

    private final SystemActivityRepository activityRepository;

    public void logActivity(String type, String message, String user) {
        try {
            SystemActivity activity = SystemActivity.builder()
                    .type(type)
                    .message(message)
                    .user(user)
                    .timestamp(LocalDateTime.now())
                    .build();
            activityRepository.save(activity);
            log.info("System activity logged: {} - {}", type, message);
        } catch (Exception e) {
            log.error("Failed to log system activity: {}", e.getMessage());
            // Fail silent to not block the main process like registration
        }
    }

    @Transactional(readOnly = true)
    public List<SystemActivity> getRecentActivities() {
        try {
            return activityRepository.findTop10ByOrderByTimestampDesc();
        } catch (Exception e) {
            log.error("Error retrieving activities: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}
