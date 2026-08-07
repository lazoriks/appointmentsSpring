package com.example.appointments.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Posts a message to a Telegram chat via the Bot API. No SDK — it's one HTTPS
 * call.
 *
 * Note a bot can only message chats that have started a conversation with it
 * (or groups it has been added to), which is why clients are notified by email
 * and only the salon's own group chat gets Telegram messages.
 */
@Component
public class TelegramSender {

    private final RestClient http = RestClient.create();
    private final String botToken;

    public TelegramSender(@Value("${app.notifications.telegram.bot-token:}") String botToken) {
        this.botToken = botToken;
    }

    public boolean isConfigured() {
        return botToken != null && !botToken.isBlank();
    }

    /** Sends a message. Throws if Telegram rejects it, so the caller can retry. */
    public void send(String chatId, String html) {
        if (!isConfigured()) {
            throw new IllegalStateException("Telegram bot token is not configured");
        }

        ResponseEntity<String> response = http.post()
                .uri("https://api.telegram.org/bot{token}/sendMessage", botToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "chat_id", chatId,
                        "text", html,
                        "parse_mode", "HTML",
                        "disable_web_page_preview", true
                ))
                .retrieve()
                .toEntity(String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Telegram returned " + response.getStatusCode()
                    + ": " + response.getBody());
        }
    }
}
