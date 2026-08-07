package com.example.appointments.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * After a booking commits, tries to deliver its queued notifications straight
 * away so the salon hears about it within seconds.
 *
 * This is best effort only: anything that fails — or that never runs because
 * Cloud Run shut the instance down — stays PENDING in the outbox and is picked
 * up by the scheduled dispatch endpoint.
 */
@Component
public class BookingCreatedListener {

    private static final Logger log = LoggerFactory.getLogger(BookingCreatedListener.class);

    private final OutboxRowSender rowSender;

    public BookingCreatedListener(OutboxRowSender rowSender) {
        this.rowSender = rowSender;
    }

    @Async
    @TransactionalEventListener
    public void onBookingCreated(BookingCreatedEvent event) {
        List<Integer> ids = event.outboxIds();
        log.info("Booking {} committed, delivering {} notification(s)", event.appointmentId(), ids.size());
        for (Integer id : ids) {
            try {
                rowSender.send(id);
            } catch (Exception e) {
                // Already recorded on the row; the scheduled dispatch will retry.
                log.warn("Immediate delivery of notification {} failed: {}", id, e.getMessage());
            }
        }
    }
}
