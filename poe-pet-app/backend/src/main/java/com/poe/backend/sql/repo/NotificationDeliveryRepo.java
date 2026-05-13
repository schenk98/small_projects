package com.poe.backend.sql.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.poe.backend.sql.model.NotificationDelivery;

/** Relational repository for notification delivery audit rows and dedupe checks. */
public interface NotificationDeliveryRepo extends JpaRepository<NotificationDelivery, Long> {
    /** Check whether the logical notification window already produced a successful delivery. */
    boolean existsByDeliveryKeyAndSuccessTrue(String deliveryKey);

    /** Load recent deliveries for a user and kind, newest first. */
    List<NotificationDelivery> findTop10ByUserIdAndKindOrderByCreatedAtDesc(String userId, String kind);
}
