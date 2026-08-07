package com.example.appointments.notification;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;

@Component
@ConditionalOnProperty(name = "spring.mail.host")
public class EmailSender {

    private final JavaMailSender mailSender;
    private final String host;
    private final String from;
    private final String fromName;

    public EmailSender(JavaMailSender mailSender,
                       @Value("${spring.mail.host:}") String host,
                       @Value("${spring.mail.username:}") String from,
                       @Value("${app.notifications.from-name:GLAM Beauty Salon}") String fromName) {
        this.mailSender = mailSender;
        this.host = host;
        this.from = from;
        this.fromName = fromName;
    }

    /** Sends an HTML email. Throws if delivery fails, so the caller can retry. */
    public void send(String to, String subject, String html) throws Exception {
        // An empty MAIL_HOST still satisfies @ConditionalOnProperty, and the
        // resulting connection error ("Authentication failed") hides the real
        // cause. Say plainly that it just isn't set up yet.
        if (host == null || host.isBlank()) {
            throw new IllegalStateException(
                    "SMTP is not configured — set MAIL_HOST, MAIL_USERNAME and MAIL_PASSWORD");
        }
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(html, true);
        try {
            helper.setFrom(from, fromName);
        } catch (UnsupportedEncodingException e) {
            helper.setFrom(from);
        }
        mailSender.send(message);
    }
}
