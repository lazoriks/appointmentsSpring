package com.example.appointments.notification;

import com.example.appointments.entity.Appointment;
import com.example.appointments.entity.Master;
import com.example.appointments.entity.NotificationOutbox;
import com.example.appointments.entity.Service;
import com.example.appointments.repository.MasterRepository;
import com.example.appointments.repository.NotificationOutboxRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Builds the messages for a new booking and queues them in the outbox.
 *
 * Called from inside the booking transaction, so the rows commit together with
 * the appointment.
 */
@Component
public class BookingNotificationService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final NotificationOutboxRepository outbox;
    private final MasterRepository masterRepo;

    private final String salonName;
    private final String salonEmail;
    private final String salonAddress;
    private final String salonPhone;
    private final String telegramChatId;

    public BookingNotificationService(
            NotificationOutboxRepository outbox,
            MasterRepository masterRepo,
            @Value("${app.notifications.salon-name:GLAM Beauty Salon}") String salonName,
            @Value("${app.notifications.salon-email:}") String salonEmail,
            @Value("${app.notifications.salon-address:43 Thomas St, Limerick, V94 KW0Y}") String salonAddress,
            @Value("${app.notifications.salon-phone:+353 87 465 1181}") String salonPhone,
            @Value("${app.notifications.telegram.chat-id:}") String telegramChatId) {
        this.outbox = outbox;
        this.masterRepo = masterRepo;
        this.salonName = salonName;
        this.salonEmail = salonEmail;
        this.salonAddress = salonAddress;
        this.salonPhone = salonPhone;
        this.telegramChatId = telegramChatId;
    }

    /** Queues the client confirmation plus the salon's email and Telegram alerts. */
    public List<NotificationOutbox> queueForNewBooking(Appointment appointment) {
        String masterName = resolveMasterName(appointment);
        String serviceList = resolveServices(appointment);
        String when = appointment.getDatatime().format(DATE) + " at " + appointment.getDatatime().format(TIME);

        String clientName = joinName(
                appointment.getClient() != null ? appointment.getClient().getFirstName() : null,
                appointment.getClient() != null ? appointment.getClient().getSurname() : null);
        String clientMobile = appointment.getClient() != null ? appointment.getClient().getMobile() : "";
        String clientEmail = appointment.getClient() != null ? appointment.getClient().getEmail() : null;

        List<NotificationOutbox> queued = new ArrayList<>();

        // ----- client confirmation -----
        if (clientEmail != null && !clientEmail.isBlank()) {
            queued.add(enqueue(appointment, NotificationOutbox.Channel.EMAIL, clientEmail,
                    "Your booking at " + salonName + " — " + when,
                    clientEmailBody(clientName, when, serviceList, masterName)));
        }

        // ----- salon inbox -----
        if (salonEmail != null && !salonEmail.isBlank()) {
            queued.add(enqueue(appointment, NotificationOutbox.Channel.EMAIL, salonEmail,
                    "New booking: " + when + " — " + clientName,
                    salonEmailBody(clientName, clientMobile, clientEmail, when, serviceList,
                            masterName, appointment)));
        }

        // ----- salon Telegram group -----
        if (telegramChatId != null && !telegramChatId.isBlank()) {
            queued.add(enqueue(appointment, NotificationOutbox.Channel.TELEGRAM, telegramChatId,
                    null,
                    telegramBody(clientName, clientMobile, when, serviceList, masterName, appointment)));
        }

        return queued;
    }

    private NotificationOutbox enqueue(Appointment appointment, NotificationOutbox.Channel channel,
                                       String recipient, String subject, String body) {
        NotificationOutbox row = new NotificationOutbox();
        row.setAppointmentId(appointment.getId());
        row.setChannel(channel);
        row.setRecipient(recipient);
        row.setSubject(subject);
        row.setBody(body);
        return outbox.save(row);
    }

    // ------------------------------------------------------------------
    // message bodies
    // ------------------------------------------------------------------

    private String clientEmailBody(String clientName, String when, String services, String master) {
        return """
                <div style="font-family:Helvetica,Arial,sans-serif;font-size:15px;color:#222;line-height:1.6">
                  <p>Hi %s,</p>
                  <p>Your appointment at <strong>%s</strong> is confirmed.</p>
                  <table style="border-collapse:collapse;margin:16px 0">
                    <tr><td style="padding:4px 12px 4px 0"><strong>When</strong></td><td>%s</td></tr>
                    <tr><td style="padding:4px 12px 4px 0"><strong>Service</strong></td><td>%s</td></tr>
                    <tr><td style="padding:4px 12px 4px 0"><strong>Beautician</strong></td><td>%s</td></tr>
                    <tr><td style="padding:4px 12px 4px 0"><strong>Where</strong></td><td>%s</td></tr>
                  </table>
                  <p>Need to change or cancel? Just call us on <a href="tel:%s">%s</a>.</p>
                  <p>See you soon!<br>%s</p>
                </div>
                """.formatted(escape(clientName), escape(salonName), escape(when), escape(services),
                escape(master), escape(salonAddress), salonPhone.replaceAll("\\s", ""),
                escape(salonPhone), escape(salonName));
    }

    private String salonEmailBody(String clientName, String mobile, String email, String when,
                                  String services, String master, Appointment appointment) {
        return """
                <div style="font-family:Helvetica,Arial,sans-serif;font-size:15px;color:#222;line-height:1.6">
                  <p><strong>New booking</strong></p>
                  <table style="border-collapse:collapse;margin:16px 0">
                    <tr><td style="padding:4px 12px 4px 0"><strong>When</strong></td><td>%s</td></tr>
                    <tr><td style="padding:4px 12px 4px 0"><strong>Client</strong></td><td>%s</td></tr>
                    <tr><td style="padding:4px 12px 4px 0"><strong>Mobile</strong></td><td><a href="tel:%s">%s</a></td></tr>
                    <tr><td style="padding:4px 12px 4px 0"><strong>Email</strong></td><td>%s</td></tr>
                    <tr><td style="padding:4px 12px 4px 0"><strong>Service</strong></td><td>%s</td></tr>
                    <tr><td style="padding:4px 12px 4px 0"><strong>Beautician</strong></td><td>%s</td></tr>
                    <tr><td style="padding:4px 12px 4px 0"><strong>Total</strong></td><td>%s</td></tr>
                  </table>
                </div>
                """.formatted(escape(when), escape(clientName), mobile.replaceAll("\\s", ""),
                escape(mobile), escape(email == null ? "—" : email), escape(services),
                escape(master), formatTotal(appointment));
    }

    private String telegramBody(String clientName, String mobile, String when, String services,
                                String master, Appointment appointment) {
        return """
                <b>New booking</b>

                🗓 %s
                👤 %s
                📞 %s
                💅 %s
                👩‍🎨 %s
                💶 %s"""
                .formatted(escape(when), escape(clientName), escape(mobile), escape(services),
                        escape(master), formatTotal(appointment));
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private String resolveMasterName(Appointment appointment) {
        Master master = appointment.getMaster();
        if (master == null || master.getId() == null) {
            return "—";
        }
        // The booking flow attaches a bare id reference, so load the real row.
        return masterRepo.findById(master.getId())
                .map(m -> joinName(m.getFirstName(), m.getSurname()))
                .orElse("—");
    }

    private String resolveServices(Appointment appointment) {
        List<Service> services = appointment.getServices();
        if (services != null && !services.isEmpty()) {
            return services.stream().map(Service::getServiceName).collect(Collectors.joining(", "));
        }
        return appointment.getService() != null ? appointment.getService().getServiceName() : "—";
    }

    private String formatTotal(Appointment appointment) {
        return appointment.getSumm() != null ? "€" + appointment.getSumm() : "—";
    }

    private String joinName(String first, String surname) {
        String name = ((first == null ? "" : first) + " " + (surname == null ? "" : surname)).trim();
        return name.isEmpty() ? "—" : name;
    }

    /** Both HTML email and Telegram's HTML parse mode need these escaped. */
    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
