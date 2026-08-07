package com.example.appointments.notification;

import com.example.appointments.entity.NotificationOutbox;
import com.example.appointments.repository.NotificationOutboxRepository;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Drains the outbox. The actual sending lives in {@link OutboxRowSender} so each
 * row gets its own transaction — one bad address can't hold up the rest, and a
 * failure is recorded rather than rolled back.
 */
@Component
public class NotificationDispatcher {

    /** Keep a single run bounded — Cloud Scheduler will come back for the rest. */
    private static final int BATCH_SIZE = 50;

    private final NotificationOutboxRepository outbox;
    private final OutboxRowSender rowSender;

    public NotificationDispatcher(NotificationOutboxRepository outbox, OutboxRowSender rowSender) {
        this.outbox = outbox;
        this.rowSender = rowSender;
    }

    /** @return how many rows were sent successfully in this run. */
    public int dispatchPending() {
        List<NotificationOutbox> pending = outbox.findByStatusAndAttemptsLessThanOrderByCreatedAtAsc(
                NotificationOutbox.Status.PENDING, NotificationOutbox.MAX_ATTEMPTS, Limit.of(BATCH_SIZE));

        int sent = 0;
        for (NotificationOutbox row : pending) {
            if (rowSender.send(row.getId())) {
                sent++;
            }
        }
        return sent;
    }

    public long pendingCount() {
        return outbox.countByStatus(NotificationOutbox.Status.PENDING);
    }

    public long failedCount() {
        return outbox.countByStatus(NotificationOutbox.Status.FAILED);
    }
}
