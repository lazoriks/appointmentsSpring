package com.example.appointments.notification;

import com.example.appointments.entity.NotificationOutbox;
import com.example.appointments.repository.NotificationOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Sends one outbox row in its own transaction, recording the outcome either way.
 *
 * Separate from {@link NotificationDispatcher} on purpose: a {@code @Transactional}
 * method called from another method of the same bean bypasses the proxy and would
 * silently run without a transaction.
 */
@Component
public class OutboxRowSender {

    private static final Logger log = LoggerFactory.getLogger(OutboxRowSender.class);

    private final NotificationOutboxRepository outbox;
    private final ObjectProvider<EmailSender> emailSender;
    private final TelegramSender telegramSender;

    public OutboxRowSender(NotificationOutboxRepository outbox,
                           ObjectProvider<EmailSender> emailSender,
                           TelegramSender telegramSender) {
        this.outbox = outbox;
        this.emailSender = emailSender;
        this.telegramSender = telegramSender;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean send(Integer id) {
        NotificationOutbox row = outbox.findById(id).orElse(null);
        if (row == null || row.getStatus() != NotificationOutbox.Status.PENDING) {
            return false;
        }

        row.setAttempts(row.getAttempts() + 1);
        try {
            switch (row.getChannel()) {
                case EMAIL -> {
                    EmailSender sender = emailSender.getIfAvailable();
                    if (sender == null) {
                        throw new IllegalStateException("SMTP is not configured (spring.mail.host)");
                    }
                    sender.send(row.getRecipient(), row.getSubject(), row.getBody());
                }
                case TELEGRAM -> telegramSender.send(row.getRecipient(), row.getBody());
            }

            row.setStatus(NotificationOutbox.Status.SENT);
            row.setSentAt(LocalDateTime.now());
            row.setLastError(null);
            outbox.save(row);
            return true;

        } catch (Exception e) {
            row.setLastError(truncate(e.getMessage()));
            if (row.getAttempts() >= NotificationOutbox.MAX_ATTEMPTS) {
                row.setStatus(NotificationOutbox.Status.FAILED);
                log.error("Notification {} to {} failed permanently after {} attempts: {}",
                        row.getId(), row.getRecipient(), row.getAttempts(), e.getMessage());
            } else {
                log.warn("Notification {} to {} failed (attempt {}), will retry: {}",
                        row.getId(), row.getRecipient(), row.getAttempts(), e.getMessage());
            }
            outbox.save(row);
            return false;
        }
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
