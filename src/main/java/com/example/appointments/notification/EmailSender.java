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
    private final String from;
    private final String fromName;

    public EmailSender(JavaMailSender mailSender,
                       @Value("${spring.mail.username:}") String from,
                       @Value("${app.notifications.from-name:GLAM Beauty Salon}") String fromName) {
        this.mailSender = mailSender;
        this.from = from;
        this.fromName = fromName;
    }

    /** Sends an HTML email. Throws if delivery fails, so the caller can retry. */
    public void send(String to, String subject, String html) throws Exception {
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
