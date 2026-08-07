package com.example.appointments.repository;

import com.example.appointments.entity.NotificationOutbox;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, Integer> {

    List<NotificationOutbox> findByStatusAndAttemptsLessThanOrderByCreatedAtAsc(
            NotificationOutbox.Status status, int maxAttempts, Limit limit);

    long countByStatus(NotificationOutbox.Status status);
}
