package com.example.appointments.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * One pending notification. Rows are written in the same transaction as the
 * appointment they describe, so a booking can never be saved without its
 * notifications being queued — and a notification can never be queued for a
 * booking that rolled back.
 *
 * Sending is then attempted immediately (best effort) and retried by
 * {@code POST /api/admin/notifications/dispatch}, which Cloud Scheduler calls
 * periodically. That retry path matters because Cloud Run scales to zero: the
 * instance that took the booking may be gone before a slow SMTP call finishes.
 */
@Entity
@Table(name = "notification_outbox")
@Data
public class NotificationOutbox {

    public enum Channel { EMAIL, TELEGRAM }

    public enum Status { PENDING, SENT, FAILED }

    /** Give up after this many attempts and leave the row as FAILED for inspection. */
    public static final int MAX_ATTEMPTS = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "appointment_id")
    private Integer appointmentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Channel channel;

    /** Email address, or the Telegram chat id. */
    @Column(nullable = false, length = 255)
    private String recipient;

    @Column(length = 255)
    private String subject;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status = Status.PENDING;

    @Column(nullable = false)
    private int attempts = 0;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
