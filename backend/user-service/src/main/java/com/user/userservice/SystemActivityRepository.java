package com.user.userservice;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SystemActivityRepository extends JpaRepository<SystemActivity, Long> {
    List<SystemActivity> findTop10ByOrderByTimestampDesc();
}
