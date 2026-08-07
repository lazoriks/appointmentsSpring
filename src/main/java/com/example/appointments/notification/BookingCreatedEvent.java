package com.example.appointments.notification;

import java.util.List;

/**
 * Published inside the booking transaction; delivered only after it commits.
 *
 * Carries just ids — by the time listeners run the transaction is over, so
 * entities would be detached.
 */
public record BookingCreatedEvent(Integer appointmentId, List<Integer> outboxIds) {
}
