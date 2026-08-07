package com.example.appointments.controller;

import com.example.appointments.notification.NotificationDispatcher;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Retry hook for queued notifications.
 *
 * Cloud Run scales to zero, so nothing runs in the background between requests —
 * a scheduled `@Scheduled` method would simply not fire. Cloud Scheduler calls
 * this endpoint instead, which wakes the service up and drains whatever is
 * still pending.
 *
 * Under /api/admin/**, so AdminApiKeyFilter already requires the X-Admin-Key
 * header; the Cloud Scheduler job sends it.
 */
@RestController
@RequestMapping("/api/admin/notifications")
public class NotificationAdminController {

    private final NotificationDispatcher dispatcher;

    public NotificationAdminController(NotificationDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @PostMapping("/dispatch")
    public Map<String, Object> dispatch() {
        int sent = dispatcher.dispatchPending();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sent", sent);
        result.put("stillPending", dispatcher.pendingCount());
        result.put("failed", dispatcher.failedCount());
        return result;
    }

    /** Queue health, for a quick look without opening the database. */
    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pending", dispatcher.pendingCount());
        result.put("failed", dispatcher.failedCount());
        return result;
    }
}
